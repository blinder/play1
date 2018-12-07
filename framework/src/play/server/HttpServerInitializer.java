package play.server;

import java.util.HashMap;
import java.util.Map;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFactory;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpObjectAggregator;
import play.Logger;
import play.Play;

public class HttpServerInitializer extends ChannelInitializer<Channel> {
	private String pipelineConfig = Play.configuration.getProperty("play.netty.pipeline",
			"play.server.FlashPolicyHandler"
			+ ",io.netty.handler.codec.http.HttpRequestDecoder"
			+ ",io.netty.handler.codec.http.HttpObjectAggregator"
			+ ",io.netty.handler.codec.http.HttpResponseEncoder"
			+ ",io.netty.handler.stream.ChunkedWriteHandler"
			+ ",play.server.PlayHandler");

    protected static Map<String, Class> classes = new HashMap<>();


	@Override
	protected void initChannel(Channel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        
        String[] handlers = pipelineConfig.split(",");  
        if(handlers.length <= 0){
            Logger.error("You must define at least the playHandler in \"play.netty.pipeline\"");
            return;
        }       
        
        // Create the play Handler (always the last one)
        String handler = handlers[handlers.length - 1];
        ChannelHandler instance = getInstance(handler);
        PlayHandler playHandler = (PlayHandler) instance;
        if (playHandler == null) {
            Logger.error("The last handler must be the playHandler in \"play.netty.pipeline\"");
            return;
        }
      
        // Get all the pipeline. Give the user the opportunity to add their own
        for (int i = 0; i < handlers.length - 1; i++) {
            handler = handlers[i];
            try {
                String name = getName(handler.trim());
                instance = getInstance(handler);
                if (instance != null) {
                    pipeline.addLast(name, instance);
                    playHandler.pipelines.put(name, instance);
                }
            } catch (Throwable e) {
                Logger.error(" error adding " + handler, e);
            }
        }
               
        if (playHandler != null) {
            pipeline.addLast("handler", playHandler);
            playHandler.pipelines.put("handler", playHandler);
        } 
    }

    protected String getName(String name) {
        if (name.lastIndexOf(".") > 0)
            return name.substring(name.lastIndexOf(".") + 1);
        return name;
    }

    protected ChannelHandler getInstance(String name) throws Exception {

        Class clazz = classes.get(name);
        if (clazz == null) {
            clazz = Class.forName(name);
            classes.put(name, clazz);
        }
        if (ChannelHandler.class.isAssignableFrom(clazz)) {
        	if (clazz.equals(HttpObjectAggregator.class)) {
        		return new HttpObjectAggregator(1048576); // no non-arg constructor available
        	}
            return (ChannelHandler)clazz.newInstance(); 
        }
        return null;
    }
}

