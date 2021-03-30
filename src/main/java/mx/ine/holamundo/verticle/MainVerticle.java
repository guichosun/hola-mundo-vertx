package mx.ine.holamundo.verticle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import mx.ine.holamundo.verticle.db.ServiceAccessVerticle;
import mx.ine.holamundo.verticle.http.HttpServerVerticle;

public class MainVerticle extends AbstractVerticle {

	private static final Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class);

	public void start(Promise<Void> promise) throws Exception {

		// Se lanza el verticle de la db
		Promise<String> dbVerticleDeployment = Promise.promise();
		vertx.deployVerticle(new ServiceAccessVerticle(), dbVerticleDeployment);

		// Se lanza el verticle de las peticiones HTTP.
		Future<String> httpVerticleDeployment = dbVerticleDeployment.future().compose(id -> {
			Promise<String> deployPromise = Promise.promise();
			vertx.deployVerticle(new HttpServerVerticle(), deployPromise);
			return deployPromise.future();
		});

		httpVerticleDeployment.onComplete(ar -> {
			if (ar.succeeded()) {
				LOGGER.info("Termina de levantar la micro-aplicacion");
				promise.complete();
			} else {
				System.out.println("fayo " + ar.cause());
				LOGGER.error(ar.cause().getMessage(),ar.cause());
			}
		});

	}
}
