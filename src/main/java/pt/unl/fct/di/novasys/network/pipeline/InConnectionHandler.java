package pt.unl.fct.di.novasys.network.pipeline;

import io.netty.channel.*;
import io.netty.util.concurrent.Promise;
import io.netty.util.concurrent.PromiseNotifier;
import pt.unl.fct.di.novasys.network.data.Attributes;
import pt.unl.fct.di.novasys.network.data.Host;
import pt.unl.fct.di.novasys.network.listeners.MessageListener;
import pt.unl.fct.di.novasys.network.listeners.InConnListener;
import pt.unl.fct.di.novasys.network.messaging.NetworkMessage;
import pt.unl.fct.di.novasys.network.userevents.HandshakeCompleted;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetSocketAddress;

public class InConnectionHandler<T> extends ConnectionHandler<T> {

    private static final Logger logger = LogManager.getLogger(InConnectionHandler.class);

    private boolean outsideUp;
    private final InConnListener<T> listener;
    
    // 添加一个Netty连接之后，  TCP通道 
    public InConnectionHandler(InConnListener<T> listener, MessageListener<T> consumer, EventLoop loop,
                               Attributes selfAttrs, MessageEncoder<T> encoder, MessageDecoder<T> decoder) {
        super(consumer, loop, true, selfAttrs);
        this.encoder = encoder;
        this.decoder = decoder;
        this.listener = listener;//监听入站通道的连接事件
        outsideUp = false;
    }
    
    
    
    
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        if (logger.isDebugEnabled()){
            logger.debug("Incoming channel active: " + ctx.channel().remoteAddress());
        }
        this.channel = ctx.channel();
        InetSocketAddress addr = (InetSocketAddress) ctx.channel().remoteAddress();
        this.peer = new Host(addr.getAddress(), addr.getPort());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        if (logger.isDebugEnabled())
            logger.debug("In connection closed: " + ctx.channel().remoteAddress().toString());
        if (outsideUp) {
            listener.inboundConnectionDown(this, null);
            outsideUp = false;
        }
    }


    
    


    @Override
    public void sendMessage(T msg, Promise<Void> promise) {
        loop.execute(() -> {
            ChannelFuture future = channel.writeAndFlush(new NetworkMessage(NetworkMessage.APP_MSG, msg));
            if(promise != null) future.addListener(new PromiseNotifier<>(promise));
        });
    }

    @Override
    public void sendMessage(T msg) {
        sendMessage(msg, null);
    }

    
    
    //关闭连接
    public void disconnect() {
        loop.execute(() -> {
            channel.flush();
            channel.close();
        });
    }
    
    
    
    //在握手结束事件触发:连接建立
    @Override
    public void internalUserEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof HandshakeCompleted) {
            this.peerAttributes = ((HandshakeCompleted) evt).getAttr();
            listener.inboundConnectionUp(this);
            outsideUp = true;
        } else
            logger.warn("Unknown user event caught: " + evt);
    }

    
    
    
    // 异常处理： 关闭连接
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (logger.isDebugEnabled())
            logger.debug("In connection exception: " + ctx.channel().remoteAddress().toString() + " " + cause);
        if (outsideUp) {
            listener.inboundConnectionDown(this, cause);
            outsideUp = false;
        }
        ctx.close();
    }


    
    
    @Override
    public String toString() {
        return "InConnectionHandler{" +
                "peer=" + peer +
                ", attributes=" + peerAttributes +
                ", channel=" + channel +
                '}';
    }
}