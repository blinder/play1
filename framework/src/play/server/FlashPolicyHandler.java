package play.server;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.CharsetUtil;

public class FlashPolicyHandler extends ByteToMessageDecoder {

    private static final String XML = "<cross-domain-policy><allow-access-from domain=\"*\" to-ports=\"*\" /></cross-domain-policy>";
    private ByteBuf policyResponse = Unpooled.copiedBuffer(XML, CharsetUtil.UTF_8);

    /**
     * Creates a handler allowing access from any domain and any port
     */
    public FlashPolicyHandler() {
        super();
    }

    /**
     * Create a handler with a custom XML response. Useful for defining your own domains and ports.
     * @param policyResponse Response XML to be passed back to a connecting client
     */
    public FlashPolicyHandler(ByteBuf policyResponse) {
        super();
        this.policyResponse = policyResponse;
    }

    @Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf buffer, List<Object> out) throws Exception {
//    protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer) throws Exception {
        if (buffer.readableBytes() < 2) {
            return;
        }

        int magic1 = buffer.getUnsignedByte(buffer.readerIndex());
        int magic2 = buffer.getUnsignedByte(buffer.readerIndex() + 1);
        boolean isFlashPolicyRequest = (magic1 == '<' && magic2 == 'p');

        if (isFlashPolicyRequest) {
            buffer.skipBytes(buffer.readableBytes()); // Discard everything
            ctx.channel().write(policyResponse).addListener(ChannelFutureListener.CLOSE);
            return;
        }

        // Remove ourselves, important since the byte length check at top can hinder frame decoding
        // down the pipeline
        ctx.pipeline().remove(this);
        buffer.readBytes(buffer.readableBytes());
    }
}
