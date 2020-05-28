import librosa
import numpy as np
import pandas as pd
from sklearn.model_selection import train_test_split
import tensorflow as tf
import math

samplingRate = 16000.0
frameLength = 1024
shiftInterval = 512
numCepstra = 13
fftSize = frameLength
preEmphasisAlpha = 0.95
lowerFilterFreq = 0.0
upperFilterFreq = samplingRate / 2.0
numMelFilters = 128
frames = []
hammingWindowList = []
MFCC = []

def freeze_session(session, keep_var_names=None, output_names=None, clear_devices=True):
    graph = session.graph
    with graph.as_default():
        freeze_var_names = list(set(v.op.name for v in tf.compat.v1.global_variables()).difference(keep_var_names or []))
        output_names = output_names or []
        output_names += [v.op.name for v in tf.compat.v1.global_variables()]
        input_graph_def = graph.as_graph_def()
        if clear_devices:
            for node in input_graph_def.node:
                node.device = ""
        frozen_graph = tf.compat.v1.graph_util.convert_variables_to_constants(
            session, input_graph_def, output_names, freeze_var_names)
        return frozen_graph

class Fft:
    def __init__(self):
        self.numPoints = 0
        self.real = []
        self.imag = []

    def calcFFT(self):
        if self.numPoints == 1:
            return

        numStages = int(math.log(self.numPoints, math.e)/math.log(2, math.e))

        halfNumPoints = self.numPoints >> 1
        j = halfNumPoints

        for i in range(1, self.numPoints - 2):
            if i < j:
                tempReal = self.real[j]
                tempImag = self.imag[j]
                self.real[j] = self.real[i]
                self.imag[j] = self.imag[i]
                self.real[i] = tempReal
                self.imag[i] = tempImag

            k = halfNumPoints

            while k <= j:
                j -= k
                k = k >> 1
            j += k

        for stage in range(1, numStages + 1):
            LE = 1
            for i in range(stage):
                LE = LE << 1

            LE2 = LE >> 1
            UR = 1.0
            UI = 0.0

            SR = math.cos(math.pi / LE2)
            SI = -math.sin(math.pi / LE2)

            for subDFT in range(1, LE2+1):
                butterfly = subDFT - 1
                while butterfly <= (self.numPoints - 1):
                    ip = butterfly + LE2

                    tempReal = self.real[ip] * UR - self.imag[ip] * UI
                    tempImag = self.real[ip] * UI + self.imag[ip] * UR
                    self.real[ip] = self.real[butterfly] - tempReal
                    self.imag[ip] = self.imag[butterfly] - tempImag
                    self.real[butterfly] += tempReal
                    self.imag[butterfly] += tempImag
                    butterfly += LE

                tempUR = UR
                UR = tempUR * SR - UI * SI
                UI = tempUR * SI + UI * SR


    def computeFFT(self, signal):
        self.numPoints = len(signal)

        self.real = signal

        self.imag = []

        for i in range(self.numPoints):
            self.imag.append(0.0)

        self.calcFFT()

        return self.real

def preEmphasis(inputSignal):
    outputSignal = [0.0]
    for n in range(1, len(inputSignal)):
        outputSignal.append(float(inputSignal[n] - preEmphasisAlpha * inputSignal[n - 1]))

    return outputSignal

def framing(inputSignal):
    numFrames = float(float(len(inputSignal)) / float(frameLength - shiftInterval))
    if (numFrames / int(numFrames)) != 1:
        numFrames = int(numFrames) + 1

    paddedSignal = []
    for i in range(len(inputSignal)):
        paddedSignal.append(inputSignal[i])
    length = len(paddedSignal)
    for i in range(length, int(numFrames) * frameLength):
        paddedSignal.append(0.0)

    for m in range(int(numFrames)):
        l = []
        for n in range(frameLength):
            l.append(paddedSignal[m * (frameLength - shiftInterval) + n])
        frames.append(l)

def hammingWindow():
    w = []
    for n in range(frameLength):
        w.append(0.54 - 0.46 * math.cos((2*math.pi*n) / (frameLength - 1)))

    for m in range(len(frames)):
        for n in range(frameLength):
            frames[m][n] *= w[n]

def magnitudeSpectrum(frame, FFT, e):
    magSpectrum = []
    FFT.computeFFT(frame)
    for k in range(len(frame)):
        magSpectrum.append(math.pow(FFT.real[k] * FFT.real[k] + FFT.imag[k] * FFT.imag[k], 0.5))

    return magSpectrum

def freqToMel(freq):
    return 2595 * math.log10(1 + freq / 700)

def inverseMel(x):
    temp = math.pow(10, x / 2595) - 1
    return 700 * temp

def centerFreq(i):
    mel =[freqToMel(lowerFilterFreq), freqToMel(samplingRate/2.0)]

    temp = mel[0] + ((mel[1] - mel[0]) / (numMelFilters + 1)) * i
    return inverseMel(temp)

def nonLinearTransformation(fbank):
    f = []
    floor = -50.0
    for i in range(len(fbank)):
        if fbank[i] == 0.0:
            f.append(float(0xfff0000000000000))
        else:
            f.append(math.log(fbank[i],math.e))
        if f[i] < floor:
            f[i] = floor
    return f

def cepCoefficients(f):
    cepc = []
    for i in range(numCepstra):
        cepc.append(0.0)

    for i in range(numCepstra):
        for j in range(1, numMelFilters + 1):
            cepc[i] += f[j - 1] * math.cos(math.pi * i / numMelFilters * (j - 0.5))
    return cepc

def melFilter(bin, cbin):
    temp = []
    for i in range(numMelFilters+2):
        temp.append(0.0)

    for k in range(1, numMelFilters+1):
        num1 = 0.0
        num2 = 0.0
        for i in range(cbin[k-1], cbin[k] + 1):
            num1 += bin[i] * ((i - cbin[k-1] + 1) / (cbin[k] - cbin[k-1] + 1))
            #print(bin[i] * ((i - cbin[k-1] + 1) / (cbin[k] - cbin[k-1] + 1)))
        for i in range(cbin[k] + 1, cbin[k+1] + 1):
            num2 += bin[i] * (1 - ((i - cbin[k]) / (cbin[k+1] - cbin[k] + 1)))

        temp[k] = num1 + num2

    fbank = []
    for i in range(numMelFilters):
        fbank.append(temp[i+1])
    return fbank

def fftBinIndices():
    cbin = []
    for i in range(numMelFilters+2):
        cbin.append(0)
    cbin[0] = int(round(lowerFilterFreq/samplingRate * fftSize))
    cbin[len(cbin) - 1] = int(fftSize / 2)

    for i in range(1, numMelFilters + 1):
        fc = centerFreq(i)

        cbin[i] = int(round(fc/samplingRate * fftSize))
    return cbin

def process(inputSignal):
    outputSignal = preEmphasis(inputSignal)


    framing(outputSignal)


    hammingWindow()


    ct = 0

    for k in range(len(frames)):
        FFT = Fft()

        bin = magnitudeSpectrum(frames[k], FFT, k)

        cbin = fftBinIndices()

        fbank = melFilter(bin, cbin)

        f = nonLinearTransformation(fbank)

        cepc = cepCoefficients(f)

        for i in range(0, numCepstra):
            if ct == 0:
                MFCC.append([cepc[i]])
            else:
                MFCC[i].append(cepc[i])
        ct = ct + 1
    return MFCC


df = pd.read_csv(r'D:\\Odevler_Dersler\\Baby_Dataset\\filelist.csv')
df.set_index('fname', inplace=True)
classes = list(np.unique(df.label))
X = []
y = np.array([])
mfccs = np.array([])
mfccss = []
i = 0
for f in df.index:
    i += 1
    signal, rate = librosa.load(r'D:\\Odevler_Dersler\\Baby_Dataset\\all\\' + f, sr=16000)
    X.append(signal)
    sig = process(signal[:rate*5])
    mfcc = np.array(sig)
    samplingRate = 16000.0
    frameLength = 1024
    shiftInterval = 512
    numCepstra = 13
    fftSize = frameLength
    preEmphasisAlpha = 0.95
    lowerFilterFreq = 0.0
    upperFilterFreq = samplingRate / 2.0
    numMelFilters = 128
    frames = []
    hammingWindowList = []
    MFCC = []

    if df.at[f, 'label'] == 1:
        y = np.append(y, 1)
    else:
        y = np.append(y, 0)
    mfcc = np.reshape(mfcc,(13*mfcc.shape[1]))
    mfccss.append(mfcc)


mfccs = np.array(mfccss)

X_train, X_test, y_train, y_test = train_test_split(mfccs, y, test_size=0.4, random_state=42)

print(X_train)

m = tf.compat.v1.keras.models.Sequential()

m.add(tf.compat.v1.keras.layers.Flatten(input_shape=(2041,)))
m.add(tf.compat.v1.keras.layers.Dense(157, activation=tf.compat.v1.nn.tanh))
m.add(tf.compat.v1.keras.layers.Dense(128, activation=tf.compat.v1.nn.tanh))
m.add(tf.compat.v1.keras.layers.Dense(128, activation=tf.compat.v1.nn.tanh))
m.add(tf.compat.v1.keras.layers.Dense(2, activation=tf.compat.v1.nn.softmax, name='output'))
m.compile(optimizer='adam',
          loss='sparse_categorical_crossentropy',
          metrics=['accuracy'])

from keras import backend as K

m.fit(X_train, y_train, epochs=24)

frozen_graph = freeze_session(K.get_session(),
                              output_names=[out.op.name for out in m.outputs])

tf.train.write_graph(frozen_graph, r'D:\\Odevler_Dersler\\Baby_Dataset', 'baby_model.pb', as_text=False)

print([node.op.name for node in m.outputs])
print([node.op.name for node in m.inputs])
