package com.tokbox.android.otsdkwrapper.signal;

import android.util.Log;

import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Threaded implementation of a signal pipe.
 * Extenders should only implement processSignal.
 */
public abstract class ThreadedSignalProtocol<OutputDataType, InputDataType>
        extends Thread
        implements SignalProtocol<OutputDataType, InputDataType> {
    protected final String LOG_TAG = getClass().getSimpleName();

    private BlockingQueue<SignalInfo<InputDataType>> mInputQueue;

    private BlockingQueue<SignalInfo<OutputDataType>> mOutputQueue;

    protected boolean mIsOpen = true;

    public ThreadedSignalProtocol() {
        mInputQueue = new LinkedBlockingQueue<SignalInfo<InputDataType>>();
        mOutputQueue = new LinkedBlockingQueue<SignalInfo<OutputDataType>>();
    }

    @Override
    final public void close() {
        mIsOpen = false;
        this.interrupt();
    }

    /**
     * Extenders should implement this method. The logic should be:
     *   - Do any process you need on the input signalInfo (uncompress, encrypt/decrypt/whatever).
     *   - Check if there's enough information to generate a application specific SignalInfo (so
     *     if you're implementing a multipart protocol, do you have enough information?)
     *   - If you have enough information, return the processed SignalInfo. Otherwise, return null
     *
     * @param signalInfo
     */
    protected abstract Collection<SignalInfo<OutputDataType>> processSignal(
            SignalInfo<InputDataType> signalInfo);

    @Override
    final public void run() {
        while (mIsOpen) {
            try {
                Log.d(LOG_TAG, "Waiting for signal data");
                Collection<SignalInfo<OutputDataType>> processedSignals = processSignal(mInputQueue.take());
                if (processedSignals != null) {
                    for(SignalInfo processedSignal: processedSignals) {
                        mOutputQueue.add(processedSignal);
                    }
                }
            } catch (InterruptedException e) {
                Log.d(LOG_TAG, "Got interrupted while waiting for data. isOpen: " + mIsOpen);
            }
        }
    }

    @Override
    final public SignalInfo<OutputDataType> read() {
        while (true) {
            try {
                return mOutputQueue.take();
            } catch (InterruptedException e) {
                if (!mIsOpen) {
                    return null;
                }
            }
        }
    }

    @Override
    final public void write(SignalInfo<InputDataType> signalInfo) {
        mInputQueue.add(signalInfo);
    }

}