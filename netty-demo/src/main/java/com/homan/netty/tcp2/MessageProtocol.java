package com.homan.netty.tcp2;

/**
 * 协议包
 *
 * @author Homan
 */
public class MessageProtocol {
    /**
     * 信息长度
     */
    private int len;
    /**
     * 信息
     */
    private byte[] content;

    public int getLen() {
        return len;
    }

    public void setLen(int len) {
        this.len = len;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }
}
