package pt.unl.fct.di.novasys.network;

import io.netty.channel.EventLoop;
import io.netty.util.concurrent.Promise;
import pt.unl.fct.di.novasys.network.data.Attributes;
import pt.unl.fct.di.novasys.network.data.Host;

public interface Connection<T> {

    Host getPeer();

    Attributes getPeerAttributes();

    Attributes getSelfAttributes();
    
    boolean isInbound();

    boolean isOutbound();
    
    EventLoop getLoop();
    
    //关闭连接
    void disconnect();
    
    
    
    
    
    
    
    
    void sendMessage(T msg, Promise<Void> p);

    void sendMessage(T msg);

    
    
    
    
    
    long getSentAppBytes();

    long getSentAppMessages();

    long getSentControlBytes();

    long getSentControlMessages();

    
    
    
    long getReceivedAppBytes();

    long getReceivedAppMessages();

    long getReceivedControlBytes();

    long getReceivedControlMessages();

}
