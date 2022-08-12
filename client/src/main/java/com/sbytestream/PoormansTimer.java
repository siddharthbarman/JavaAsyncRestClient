package com.sbytestream;

public class PoormansTimer {
    public void start() {
        m_start = System.currentTimeMillis();
    }

    public long stop() throws Exception {
        if (m_start == 0) {
            throw new Exception("Timer hasn't been started");
        }
        m_stop = System.currentTimeMillis();
        return m_stop - m_start;
    }

    public long elapsed() throws Exception {
        if (m_start == 0) {
            throw new Exception("Timer hasn't been started");
        }

        if (m_stop == 0) {
            throw new Exception("Timer hasn't been stopped");
        }

        return m_stop - m_start;
    }

    private long m_start = 0;
    private long m_stop = 0;
}
