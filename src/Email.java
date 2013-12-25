/**
 * Created by liyuwei on 13-12-17.
 */
public class Email {
    private String from;
    private String to;
    private String subject;
    private String content;
    private String data;

    public Email(String from, String to, String subject, String content){
        this.from = from;
        this.to = to;
        this.subject = subject;
        this.content = content;
        this.data = "Subject:"+this.subject+"\r\n"+this.content;

    }

    public String getFrom(){
        return this.from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo(){
        return this.to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getSubject(){
        return this.subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getContent(){
        return this.content;
    }
    public void setContent(String content) {
        this.content = content;
    }

    public String getData(){
        return this.data;
    }
}
