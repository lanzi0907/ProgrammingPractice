import sun.misc.BASE64Encoder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.StringTokenizer;

/**
 * Created by liyuwei on 13-12-17.
 */
public class MailSender {
    private String smtpServer = "smtp.163.com";
    private int smtpPort = 25;
    private BufferedReader br;
    private PrintWriter pw;

    private String username =  "goldfish0727@163.com";
    private String passwd = "13240338226.";

    Socket socket;

    public MailSender(){
        try {
            socket = new Socket(smtpServer,smtpPort);
            br = simpleClient.getReader(socket);
            pw = simpleClient.getWriter(socket);
        } catch (Exception e){
            e.printStackTrace();
        } finally {

        }

    }

    public void sendMail(Email mail){
        try{
            String un = new BASE64Encoder().encode(this.username.getBytes());
            String passw = new BASE64Encoder().encode(this.passwd.getBytes());

            register();
            login(un, passw);


            sendAndReceive("MAIL FROM:<"+mail.getFrom()+">", pw);
            expectResult(250);
            sendAndReceive("RCPT TO: <"+mail.getTo()+">", pw);
            expectResult(250);
            sendAndReceive("DATA", pw);
            if(expectResult(354)){
              pw.println(mail.getData());
              sendAndReceive(".", pw);
              expectResult(250);
              sendAndReceive("QUIT", pw);
                expectResult(250);
            }
//            sendAndReceive(mail.getData(), pw);
////            pw.println(mail.getData());
////            System.out.println("=====client>"+mail.getData());
//
//            sendAndReceive(".", pw);
//            sendAndReceive("QUIT", pw);

        } catch (Exception e){
            e.printStackTrace();
        } finally {
            try{
                if (socket != null){
                    socket.close();
                }
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    private boolean expectResult (int exptCode) throws IOException {
        int result;
        while((result = readServerResponse(br)) != exptCode){
            System.out.println("== result == "+result);
        }
        System.out.println("== result equal to the expected one == "+result);
        return true;
    }
    private void register() throws IOException {
        sendAndReceive(null, pw);
        if(expectResult(220)){
            System.out.println("-- register success! -- ");
        } else {
            System.out.println("-- register error! -- ");
        }
    }

    private void login(String usr, String pwd) throws IOException {
        sendAndReceive("EHLO "+smtpServer, pw);
        if(expectResult(250)){
            sendAndReceive("AUTH LOGIN", pw);
            if(expectResult(250)){
                System.out.println("-- Login start success! -- ");
                sendAndReceive(usr, pw);
                if(expectResult(250)){
                    sendAndReceive(pwd, pw);
                    if(expectResult(235)){
                        System.out.println("-- Authentication success! -- ");
                        return;
                    }
                }
            }
        }
    }

    private void sendAndReceive(String str, PrintWriter pw)
    throws IOException {
        if (str != null){
            System.out.println("Client>" + str);
            pw.println(str);
        }

    }

    private int readServerResponse(BufferedReader br) throws IOException {
        String response = br.readLine();
        if(response != null){
            System.out.println("Server>>"+response);
        }

        // 从服务器返回消息中取得状态码,并转换成整数返回
        //StringTokenizer get = new StringTokenizer(response, " ");
        String result = response.substring(0,3);
//        return Integer.parseInt(get.nextToken());
        return Integer.parseInt(result);
    }
}
