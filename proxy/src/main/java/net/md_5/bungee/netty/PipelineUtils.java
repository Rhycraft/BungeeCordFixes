package net.md_5.bungee.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.AttributeKey;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.BungeeServerInfo;
import net.md_5.bungee.ServerConnector;
import net.md_5.bungee.UserConnection;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.protocol.MinecraftCodec;
import net.md_5.bungee.protocol.Protocol;

public class PipelineUtils
{

    public static final AttributeKey<ListenerInfo> LISTENER = new AttributeKey<>( "ListerInfo" );
    public static final AttributeKey<UserConnection> USER = new AttributeKey<>( "User" );
    public static final AttributeKey<BungeeServerInfo> TARGET = new AttributeKey<>( "Target" );
    public static final ChannelInitializer<Channel> SERVER_CHILD = new ChannelInitializer<Channel>()
    {
        @Override
        protected void initChannel(Channel ch) throws Exception
        {
            if ( BungeeCord.getInstance().getConnectionThrottle().throttle( ( (InetSocketAddress) ch.remoteAddress() ).getAddress() ) )
            {
                // TODO: Better throttle - we can't throttle this way if we want to maintain 1.7 compat!
                // ch.close();
                // return;
            }

            BASE.initChannel( ch );
            ch.pipeline().get( HandlerBoss.class ).setHandler( new InitialHandler( ProxyServer.getInstance(), ch.attr( LISTENER ).get() ) );
        }
    };
    public static final ChannelInitializer<Channel> CLIENT = new ChannelInitializer<Channel>()
    {
        @Override
        protected void initChannel(Channel ch) throws Exception
        {
            BASE.initChannel( ch );
            ch.pipeline().get( HandlerBoss.class ).setHandler( new ServerConnector( ProxyServer.getInstance(), ch.attr( USER ).get(), ch.attr( TARGET ).get() ) );
        }
    };
    public static final Base BASE = new Base();
    private static final ProtobufVarint32FrameDecoder frameDecoder = new ProtobufVarint32FrameDecoder();
    private static final ProtobufVarint32FrameDecoder framePrepender = new ProtobufVarint32FrameDecoder();
    public static String TIMEOUT_HANDLER = "timeout";
    public static String PACKET_CODEC = "packet-codec";
    public static String BOSS_HANDLER = "inbound-boss";
    public static String ENCRYPT_HANDLER = "encrypt";
    public static String DECRYPT_HANDLER = "decrypt";
    public static String FRAME_DECODER = "frame-decoder";
    public static String FRAME_PREPENDER = "frame-prepender";

    public final static class Base extends ChannelInitializer<Channel>
    {

        @Override
        public void initChannel(Channel ch) throws Exception
        {
            try
            {
                ch.config().setOption( ChannelOption.IP_TOS, 0x18 );
            } catch ( ChannelException ex )
            {
                // IP_TOS is not supported (Windows XP / Windows Server 2003)
            }

            ch.pipeline().addLast( TIMEOUT_HANDLER, new ReadTimeoutHandler( BungeeCord.getInstance().config.getTimeout(), TimeUnit.MILLISECONDS ) );
            ch.pipeline().addLast( FRAME_DECODER, frameDecoder );
            ch.pipeline().addLast( PACKET_CODEC, new MinecraftCodec( Protocol.SERVER_HANDSHAKE ) );
            ch.pipeline().addLast( FRAME_PREPENDER, framePrepender );
            ch.pipeline().addLast( BOSS_HANDLER, new HandlerBoss() );
        }
    };
}
