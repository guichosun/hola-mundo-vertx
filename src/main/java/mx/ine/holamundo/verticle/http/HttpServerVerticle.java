package mx.ine.holamundo.verticle.http;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.LoggerHandler;
import io.vertx.ext.web.handler.ResponseContentTypeHandler;

public class HttpServerVerticle extends AbstractVerticle {

	private static final Logger LOGGER = LoggerFactory.getLogger(HttpServerVerticle.class);


	protected HttpServer server;
	protected Router mainRouter;

	@Override
	public void start(Promise<Void> promise) throws Exception {
		super.start();
		LOGGER.debug("Se deploya HttpServerVerticle");

		HttpServerOptions options = new HttpServerOptions();
		  options
		      .setSsl(true)
//		      Opciones para el certificado al server
//		      .setClientAuth(ClientAuth.REQUIRED)
//		      .setPemKeyCertOptions(serverCert.keyCertOptions())
//		      .setTrustOptions(VertxTrustOptions.trustClientOnFirstAccess(knownClientsFile, false))
		      .setIdleTimeout(1500)
		      .setReuseAddress(true)
		      .setPort(8080)
		      .setReusePort(true);

		// Se crea el server
		server = vertx.createHttpServer(options);

		// Create a router object.
		mainRouter = Router.router(vertx);

		LOGGER.debug("Se agregan los handlers.");
		mainRouter.route().handler(BodyHandler.create());
		mainRouter.route().handler(ResponseContentTypeHandler.create());

		// El manejador de CORS
		mainRouter.route().handler(CorsHandler.create("*").allowedMethod(HttpMethod.POST).allowedMethod(HttpMethod.GET)
				.allowedMethod(HttpMethod.PUT).allowedMethod(HttpMethod.DELETE).allowedMethod(HttpMethod.OPTIONS)
				.allowedHeaders(new HashSet<String>(Arrays.asList("", "Access-Control-Allow-Method",
						"Access-Control-Allow-Origin", "Access-Control-Allow-Credentials",
						"Access-Control-Allow-Headers", "Content-Type", "Authorization", "X-Requested-With",
						"x-auth-token", "Content-Length", "Accept", "Origin"))));

		mainRouter.route().handler(BodyHandler.create());
		mainRouter.route().handler(LoggerHandler.create(LoggerHandler.DEFAULT_FORMAT));

		// DONE health-check servicio para dar de alta en el balanceador
		mainRouter.get("/test/alumnos").handler(this::doCheck);

		mainRouter.get("/api/vertx/mensaje").produces("application/json").handler(this::doCheck);

		// Configuracion del servidor.
		server.requestHandler(mainRouter::handle).listen(
				ar -> {
					if (ar.succeeded()) {
						LOGGER.debug("HTTP Server corriendo on ip {}", "localhost");
						LOGGER.debug("HTTP Server corriendo on port {}", ar.result().actualPort());

						LOGGER.debug("Se inicializa el Verticle");
						promise.complete();
					} else {
						LOGGER.error("No se puede start a HTTP server " + ar.cause());
						promise.fail(ar.cause());
					}
				});
	}

	public void doCheck(RoutingContext routingContext) {
		LOGGER.info("**********Health******");
		HttpServerResponse response = routingContext.response();
		response.putHeader("Content-Type", "application/json");
		response.setStatusCode(200);
		SimpleDateFormat sdfComplete = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		sdfComplete.setTimeZone(TimeZone.getTimeZone("America/Mexico_City"));
		String date = sdfComplete.format(new Date());
		JsonObject resp = new JsonObject().put("Estatus", "OK").put("Version", "1.0").put("date", date);
		response.end(resp.encodePrettily());
	}
}
