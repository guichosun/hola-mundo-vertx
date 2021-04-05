package mx.ine.holamundo.verticle.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;

public class ServiceAccessVerticle extends AbstractVerticle {

	private final Logger LOGGER = LoggerFactory.getLogger(ServiceAccessVerticle.class);
	/*
	 * (non-Javadoc)
	 * 
	 * @see io.vertx.core.AbstractVerticle#start(io.vertx.core.Future)
	 */
	@Override
	public void start(Promise<Void> promise) throws Exception {
		super.start(promise);
		JsonObject config = getConfig(vertx);
		JDBCClient.create(vertx, config);
		LOGGER.info("ServiceAccessVerticle de {}.", "BD");

	}

	@Override
	public void stop(Promise<Void> promise) throws Exception {

		
	}
	
	private static JsonObject getConfig(Vertx vertx) {
		ConfigStoreOptions file = new ConfigStoreOptions()
				.setType("file").setConfig(
						new JsonObject().put("path", "application.json"));
		
		ConfigRetriever retriever = ConfigRetriever.create(vertx, new ConfigRetrieverOptions().addStore(file));
		
		retriever.getConfig(conf -> {
			JsonObject datasourceConfig = conf.result().getJsonObject("port");
			System.out.println("config del application.json. "+datasourceConfig);
		});
		JsonObject config = retriever.getCachedConfig();
		return config;
	}
}
