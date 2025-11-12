package com.aisearch.dto;

public class ResponseCommon<T> {
    private int code;          // 状态码
    private T content;         // 返回内容
    private long count;        // 数量
    private Message msg;       // 消息体

    public ResponseCommon() {}

    public ResponseCommon(int code, T content, long count, Message msg) {
        this.code = code;
        this.content = content;
        this.count = count;
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public T getContent() {
        return content;
    }

    public void setContent(T content) {
        this.content = content;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public Message getMsg() {
        return msg;
    }

    public void setMsg(Message msg) {
        this.msg = msg;
    }

    // 内部类或单独放在另一个文件里
    public static class Message {
        private String success;
        private String fail;

        public Message() {}

        public Message(String success, String fail) {
            this.success = success;
            this.fail = fail;
        }

        public String getSuccess() {
            return success;
        }

        public void setSuccess(String success) {
            this.success = success;
        }

        public String getFail() {
            return fail;
        }

        public void setFail(String fail) {
            this.fail = fail;
        }
    }
}
