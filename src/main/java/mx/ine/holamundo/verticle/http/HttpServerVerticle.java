package mx.ine.holamundo.verticle.http;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.api.validation.HTTPRequestValidationHandler;
import io.vertx.ext.web.api.validation.ValidationException;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.LoggerHandler;
import io.vertx.ext.web.handler.ResponseContentTypeHandler;

public class HttpServerVerticle extends AbstractVerticle {

	private static final Logger LOGGER = LoggerFactory.getLogger(HttpServerVerticle.class);
	private WebClient clienteWeb = null;
	
	protected HttpServer server;
	protected Router mainRouter;

	@Override
	public void start(Promise<Void> promise) throws Exception {
		super.start();
		LOGGER.debug("Se deploya HttpServerVerticle");

		HttpServerOptions options = new HttpServerOptions();
		  options
		      //.setSsl(true) Key/certificate is mandatory for SSL
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

		// Las opciones del cliente
		WebClientOptions wcOptions = new WebClientOptions();
		wcOptions.setKeepAlive(true);
		wcOptions.setMaxPoolSize(100).setPipelining(true);
		wcOptions.setSsl(false);
		
		// El cliente necesario para conectar al servicio FCM.
		clienteWeb = WebClient.create(vertx, wcOptions);

		LOGGER.debug("Se agregan los handlers.");
		mainRouter.route().handler(BodyHandler.create());
		mainRouter.route().handler(ResponseContentTypeHandler.create());

		HTTPRequestValidationHandler validationHandler = HTTPRequestValidationHandler.create();

		// El manejador de CORS
		mainRouter.route().handler(CorsHandler.create("*").allowedMethod(HttpMethod.POST).allowedMethod(HttpMethod.GET)
				.allowedMethod(HttpMethod.PUT).allowedMethod(HttpMethod.DELETE).allowedMethod(HttpMethod.OPTIONS)
				.allowedHeaders(new HashSet<String>(Arrays.asList("", "Access-Control-Allow-Method",
						"Access-Control-Allow-Origin", "Access-Control-Allow-Credentials",
						"Access-Control-Allow-Headers", "Content-Type", "Authorization", "X-Requested-With",
						"x-auth-token", "Content-Length", "Accept", "Origin"))));

		mainRouter.route().handler(LoggerHandler.create(LoggerHandler.DEFAULT_FORMAT));

		// DONE health-check servicio para dar de alta en el balanceador
		mainRouter.get("/test").handler(this::doCheck);

		mainRouter.post("/api/vertx/mensaje")
//			El Validador de body
			.handler(validationHandler
				.addJsonBodySchema(bodyMensaje()))
			.produces("application/json")
			.failureHandler((routingContext) -> {
				Throwable failure = routingContext.failure();
				fail(routingContext, failure);
			})
			.handler(this::handlerMessage);

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

	private void handlerMessage(RoutingContext routingContext) {
		LOGGER.info("Entra al handler de los mensajes");

		JsonObject reqJson = routingContext.getBodyAsJson();
		LOGGER.debug("EL getBodyAsJson {}",reqJson);

		Integer id = reqJson.getInteger("id-msg");
		String message = reqJson.getString("descripcion");
		
		LOGGER.debug("Llamar al cliente web en una llamada sincrona");
		
			if (id != null) {
			
			
			} else {
				LOGGER.debug("Esta vacio el body");
				JsonObject jsonData = new JsonObject();
						jsonData.put("resultado", "El id-msg viene vacio");
				
				HttpServerResponse response = routingContext.response();
				response.putHeader("Content-Type", "application/json; charset=utf-8");
				response.putHeader("Access-Control-Allow-Origin", "*");
				response.setStatusCode(HttpResponseStatus.BAD_REQUEST.code());
				response.end(jsonData.encodePrettily());
			}	
		}

		/**
		 * El schema del body de las notificaciones de las tareas.
		 * @return
		 */
		public static String bodyMensaje() {
			StringBuffer schema = new StringBuffer();
			schema.append("{")
			
			.append("\"description\": \"\",")
			.append("\"type\": \"object\",")
			.append("\"properties\": { ")
			.append("		\"id-msg\": { ")
			.append("			\"description\": \"El id\",") 
			.append("			\"type\": \"integer\"")
			.append("		},")
			.append("		\"descripcion\": { ")
			.append("			\"description\": \"Una descripcion\",") 
			.append("			\"type\": \"string\"")
			.append("		}")
			.append("	}, ")
			.append("\"required\": [ \"nombre\"]")
			.append("}");
			return schema.toString();
		}
		
		protected void fail(RoutingContext routingContext, Throwable exception) {
			HttpServerResponse response = routingContext.response();
			response.putHeader("Content-Type", "application/json");
			response.putHeader("Access-Control-Allow-Origin", "*");
			response.setStatusCode(HttpResponseStatus.NOT_ACCEPTABLE.code());
			
			JsonObject failureResponse = new JsonObject();
			
			
			if (exception instanceof DecodeException) {
				failureResponse.put("resultado", "Error de decode");
				response.end(JsonObject.mapFrom(failureResponse).encodePrettily());
				response.setStatusCode(HttpResponseStatus.NOT_ACCEPTABLE.code());
			} else if (exception instanceof ValidationException) {
				failureResponse.put("resultado", "Fallo de validación");
				ValidationException ve = (ValidationException)exception;
				LOGGER.debug("EL tipo del error {}",ve.type());
			
				response.setStatusCode(HttpResponseStatus.BAD_REQUEST.code());
				response.end(JsonObject.mapFrom(failureResponse).encodePrettily());
			
				
			} else {
				
				failureResponse.put("resultado", "Error desconocido");
				LOGGER.debug("EL error es: {}",exception.getMessage());
				response.setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
					response.end(JsonObject.mapFrom(failureResponse).encodePrettily());	
				}
				
			}
}
