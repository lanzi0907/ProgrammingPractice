import java.io.*;
import java.net.Socket;

/**
 * Created with IntelliJ IDEA.
 * User: liyuwei
 * Date: 13-12-11
 * Time: 下午8:14
 * To change this template use File | Settings | File Templates.
 */
public class simpleClient {
    private String serverHost = "localhost";
    private int serverPort = 8000;
    private Socket socket;

    public simpleClient(){
        try {
            socket = new Socket(serverHost, serverPort);
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    static public PrintWriter getWriter(Socket socket) throws IOException {
        OutputStream socketOutput = socket.getOutputStream();
        return new PrintWriter(socketOutput,true);
    }

    static public BufferedReader getReader(Socket socket) throws IOException {
        InputStream socketIn = socket.getInputStream();
        return new BufferedReader(new InputStreamReader(socketIn));
    }

    public void connect(){
        try {
            BufferedReader br = getReader(socket);
            PrintWriter pw = getWriter(socket);
            BufferedReader localReader = new BufferedReader(new InputStreamReader(System.in));

            String msg = null;

            while((msg=localReader.readLine())!=null){
                pw.println(msg);
                System.out.println("The message is: "+msg);
                System.out.println("The BR is: "+br.readLine());

                if(msg.equals("bye")){
                    break;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } finally {
            try{
                socket.close();
            } catch (IOException e){
                e.printStackTrace();
            }
        }
    }
    public static void main(String[] args){
        System.out.println("Hi, this is a client");
//        simpleClient client = new simpleClient();
//        client.connect();
        Email mymail = new Email("goldfish0727@163.com",
                "tracy_lee0907@hotmail.com",
                "Hello Kitty","I am a mad NiuZi");

        MailSender sender = new MailSender();
        sender.sendMail(mymail);

        /**
         * Test the sample code

        MailMessage message = new MailMessage();
        message.setFrom("goldfish0727@163.com");
        message.setTo("tracy_lee0907@hotmail.com");
        message.setSubject("这个是一个邮件发送测试");
        message.setUser("goldfish0727@163.com");
        message.setContent("Hello,this is a mail send test\n你好这是一个邮件发送测试");
        message.setDatafrom("GoldFish");
        message.setDatato("LiYuwei");
        message.setPassword("13240338226.");

        SendMail send = SendMailImpl.getInstance(SendMailImpl.WANGYI163).setMessage(message);
        try {
            send.sendMail();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        */
    }
}
