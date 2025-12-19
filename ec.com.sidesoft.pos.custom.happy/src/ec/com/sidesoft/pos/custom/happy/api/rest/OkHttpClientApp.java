package ec.com.sidesoft.pos.custom.happy.api.rest;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.businesspartner.Location;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;
import org.openbravo.model.financialmgmt.payment.PaymentTerm;
import org.openbravo.model.pricing.pricelist.PriceList;
import org.openbravo.retail.posterminal.OBPOSApplications;

import com.google.gson.JsonObject;

import ec.com.sidesoft.happy.web.services.Shws_Log;
import ec.com.sidesoft.pos.custom.happy.SigMasterData;
import ec.com.sidesoft.pos.custom.happy.SigUrlApi;
import ec.com.sidesoft.retail.custompos.CSCPS_GuaranteeType;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Interceptor;
import okhttp3.Interceptor.Chain;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

public class OkHttpClientApp {

  public static Date fecha = new Date(Calendar.getInstance().getTimeInMillis());
  public static SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
  private static final Logger log = Logger.getLogger(OkHttpClientApp.class);

  private static SigUrlApi urlApi = null;
  
  private static final OkHttpClient httpClient = new OkHttpClient.Builder()
	        .connectTimeout(15, TimeUnit.SECONDS)
	        .readTimeout(20, TimeUnit.SECONDS)
	        .writeTimeout(20, TimeUnit.SECONDS)
	        .retryOnConnectionFailure(true)
	        .build();


  public static JSONObject execConsultarPoliticaComercial(final String cedula, final String ReferenceN0,
      final String terminalId) {

    OBPOSApplications obposApplications = OBDal.getInstance().get(OBPOSApplications.class,
        terminalId);

    Organization org = obposApplications.getOrganization();
    final String tienda = org.getSigTienda();
    final String usuario = org.getSigUser();

    final String msg = String.format(OBMessageUtils.messageBD("SIG_ConsultarPoliticaComercial"),
        cedula, usuario, tienda);
    log.info(msg);
    urlApi = getSigUrlApiEntity();
    
    final String sigToken = urlApi.getCscpsTokenSigma()+"";
    final String url = urlApi.getUrlCedula();
    
    try {
    	
    // Service 1
    JSONObject jsonBody = new JSONObject();
    jsonBody.put("CEDULA", cedula);
    jsonBody.put("USUARIO", usuario);
    jsonBody.put("TOKEN", sigToken);
    jsonBody.put("TIENDA", tienda);
    String ReferenceNo = ReferenceN0;
    Log_Register newLog = new Log_Register();
    Shws_Log objLog = newLog.startLog(1, ReferenceNo, url, jsonBody.toString());

    RequestBody body = new MultipartBody.Builder().setType(MultipartBody.FORM)
        .addFormDataPart("CEDULA", cedula).addFormDataPart("USUARIO", usuario)
        .addFormDataPart("TOKEN", sigToken).addFormDataPart("TIENDA", tienda).build();

    Request request = new Request.Builder().url(url).method("POST", body)
        .addHeader("Cookie", "ci_session=osjcr3dv7o6hsaos6ne8g4hvl9aghfmr").build();

      Response response = httpClient.newCall(request).execute();


      final String responseApi = response.body().string();
      log.info("**Respuesta Consultar API Politica Comercial:" + responseApi);
      
      String result = "ERROR";
      try {
    	  result = validateCodeResponse(response, responseApi);
      } catch(Exception e) {
    	  result = "ERROR";
      }
      
      newLog.endLog(objLog, responseApi, result);

      JSONObject responseJson = new JSONObject(responseApi);

      // Crear cliente
      if (responseJson.has("cliente")) {
        log.info("Creado cliente");
        BusinessPartner newBusinessPartner = createBusinessPartner(obposApplications,responseJson.getJSONObject("cliente"));
        validationsPartnerPOS(newBusinessPartner, cedula);
        	Location locationBP = newBusinessPartner.getBusinessPartnerLocationList().get(0);
            JSONObject newBusinessPartnerJSONObject = new JSONObject();
            newBusinessPartnerJSONObject.put("bpId", newBusinessPartner.getId());
            newBusinessPartnerJSONObject.put("locationBPId", locationBP.getId());
            newBusinessPartnerJSONObject.put("bpIsNew", newBusinessPartner.isNewOBObject());
            responseJson.put("bPartner", newBusinessPartnerJSONObject);
      }

      log.info("***Fin Consultar API Politica Comercial***");

      return responseJson;

    } catch (IOException e) {
      throw new OBException("Consultar Politica Comercial: " + e.getMessage());
    } catch (JSONException e) {
      throw new OBException("Consultar Politica Comercial: " + e.getMessage());
    }
  }
  
  public static void validationsPartnerPOS(BusinessPartner newBusinessPartner, String cedula) {
	  List<Location> currentLocation = newBusinessPartner.getBusinessPartnerLocationList();//Address
      if(currentLocation.size() <= 0) {
    	  throw new OBException("Cliente con cédula "+cedula+" no tiene dirección.");
      }
	  PriceList priceList = newBusinessPartner.getPriceList(); //Fee
      if(priceList == null || priceList.getId()== null || priceList.getId().equals("")) {
      	throw new OBException("El usuario "+cedula+" no tiene tarifa de venta.");
      }
      FIN_PaymentMethod payMethod = newBusinessPartner.getPaymentMethod(); //Method payment
      if(payMethod == null || payMethod.getId()== null || payMethod.getId().equals("")) {
      	throw new OBException("El usuario "+cedula+" no tiene método de pago.");
      }
      PaymentTerm payTerm = newBusinessPartner.getPaymentTerms(); //Terms payment
      if(payTerm == null || payTerm.getId()== null || payTerm.getId().equals("")) {
      	throw new OBException("El usuario "+cedula+" no tiene codiciones de pago.");
      }
  }

  public static JSONObject execCalculadora(final String cedula, final String monto,
      final String entrada, final String numero_autorizacion, final String ReferenceN0, final String terminalId, final String candado, final String guaranteeValue) {
    try {

      OBPOSApplications obposApplications = OBDal.getInstance().get(OBPOSApplications.class,
          terminalId);
      Organization org = obposApplications.getOrganization();
      final String tienda = org.getSigTienda();

      urlApi = getSigUrlApiEntity();
      final String urlApiString = urlApi.getUrlSimulacion();

      log.info(OBMessageUtils.messageBD("SIG_Calculadora") + " CEDULA:" + cedula + " AUTORIZACION:"
          + numero_autorizacion + " TIENDA:" + tienda + " MONTO:" + monto + " ENTRADA:" + entrada
          + " FECHA CONSULTA:" + formatter.format(fecha)+ " CANDADO:"+candado+" GARANIA:"+ guaranteeValue );

      CSCPS_GuaranteeType objGuarantee = OBDal.getInstance().get(CSCPS_GuaranteeType.class, guaranteeValue);
      String nameGuarantee = "";
      if (objGuarantee != null) {
	nameGuarantee = objGuarantee.getName();
      }

      // Service 2
      JSONObject jsonBody = new JSONObject();
      jsonBody.put("CEDULA", cedula);
      jsonBody.put("AUTORIZACION", numero_autorizacion);
      jsonBody.put("TOKEN", "10112011");
      jsonBody.put("TIENDA", tienda);
      jsonBody.put("MONTO", monto);
      jsonBody.put("ENTRADA", entrada);
      jsonBody.put("FECHA_CONSULTA", formatter.format(fecha));
      jsonBody.put("CANDADO", candado);
      jsonBody.put("GARANTIA", nameGuarantee);
      String ReferenceNo = ReferenceN0;
      Log_Register newLog = new Log_Register();
      Shws_Log objLog = newLog.startLog(2, ReferenceNo, urlApiString, jsonBody.toString());

      
      MediaType mediaType = MediaType.parse("text/plain");
      RequestBody body = new MultipartBody.Builder().setType(MultipartBody.FORM)
          .addFormDataPart("CEDULA", cedula).addFormDataPart("AUTORIZACION", numero_autorizacion)
          .addFormDataPart("TOKEN", "10112011").addFormDataPart("TIENDA", tienda)
          .addFormDataPart("MONTO", monto).addFormDataPart("ENTRADA", entrada)
          .addFormDataPart("FECHA_CONSULTA", formatter.format(fecha))
          .addFormDataPart("CANDADO", candado)
          .addFormDataPart("GARANTIA", nameGuarantee).build();

      Request request = new Request.Builder().url(urlApiString).method("POST", body)
          .addHeader("Cookie", "ci_session=2e9oa4gius9i82e1ngdbg6jio2kh57r5").build();
      Response response = httpClient.newCall(request).execute();

      final String responseApi = response.body().string();
      log.info("**Respuesta consulta api calculadora:" + responseApi);
      
      String result = "ERROR";
      try {
    	  result = validateCodeResponse(response, responseApi);
      } catch(Exception e) {
    	  result = "ERROR";
      }
      
      newLog.endLog(objLog, responseApi, result);

      JSONObject responseJson = new JSONObject(responseApi);
      log.info("***Fin consulta api calculadora***");
      
      JSONObject jsonMsg = new JSONObject();
      if(urlApi  != null &&  urlApi.getCscpsCalculatorMsg() != null  &&  !urlApi.getCscpsCalculatorMsg().equals("")) {
    	  jsonMsg.put("msg", true);
    	  jsonMsg.put("msgText", urlApi.getCscpsCalculatorMsg());
      } else {
    	  jsonMsg.put("msg",false);
      }
      responseJson.put("jsonMsg", jsonMsg);

      return responseJson;
    } catch (Exception e) {
      throw new OBException("Error consulta api calculadora: " + e.getMessage());
    }
  }

  public static JSONObject exeValidaCandadoTrustonic(final String imai, final String telefono, final String ReferenceN0) {
	  Integer codeResponse = 0;
    try {

      JSONObject responseJson = new JSONObject();

      urlApi = getSigUrlApiEntity();
      final String urlApiString = urlApi.getVcandadoUrlTrustonic();
      final String tokenApiString = urlApi.getCscpsTokenTrustonic();

      final String msg = String.format(OBMessageUtils.messageBD("SIG_ValidaCandado"),
          imai.toString());
      log.info(msg);
      OkHttpClient client = new OkHttpClient().newBuilder().build();
      MediaType mediaType = MediaType.parse("application/json");
      JSONObject mediaTypeJson = new JSONObject();
      JSONArray jsonArray = new JSONArray();

      jsonArray.put(new JSONObject().put("imei", imai.toString()));
      mediaTypeJson.put("devices", jsonArray);
      
      // Service 6
      String ReferenceNo = ReferenceN0;
      Log_Register newLog = new Log_Register();
      Shws_Log objLog = newLog.startLog(6, ReferenceNo, urlApiString, mediaTypeJson.toString()); 

      @SuppressWarnings("deprecation")
      RequestBody body = RequestBody.create(mediaType, mediaTypeJson.toString());
      Request request = new Request.Builder().url(urlApiString).method("POST", body)
          .addHeader("Content-Type", "application/json")
          .addHeader("Authorization",
              "Bearer "+tokenApiString)
          .build();
      Response response = httpClient.newCall(request).execute();
      
      final String responseApi = response.body().string();
      log.info("**Respuesta consulta api valida candado:" + responseApi);
      
      
      
      String result = "ERROR";
      try {
    	  result = validateCodeResponse(response, responseApi);
      } catch(Exception e) {
    	  result = "ERROR";
      }
      
      newLog.endLog(objLog, responseApi, result);
      
      codeResponse = response.code();
      if(codeResponse == 404 || codeResponse ==401) {
    	  throw new OBException("Error de servicio"); 
      }
      
      responseJson = new JSONObject(responseApi);

      log.info("***Fin consulta api valida candado***");
      return responseJson;

    } catch (Exception e) {
      log.info("Error consulta api valida candado: " + e.getMessage());
      
      if(codeResponse == 404) {
    	  throw new OBException("URL incorrecta para Trustonic"); 
      }
      if(codeResponse == 401) {
    	  throw new OBException("Token inválido para el servicio Trustonic"); 
      }
      
      throw new OBException("Error al intentar valida candado: " + e.getMessage());
    }

  }

  public static JSONObject exeValidaCandadoNuopay(final String imai, final String telefono, final String ReferenceN0) {
	  Integer codeResponse = 0;
    try {

      JSONObject responseJson = new JSONObject();
      
      urlApi = getSigUrlApiEntity();
      final String urlApiString = urlApi.getUrlValidaCandado();
      final String tokenApiString = urlApi.getCscpsTokenNuovopay();
      
      // Service 7
      String ReferenceNo = ReferenceN0;
      Log_Register newLog = new Log_Register();
      Shws_Log objLog = newLog.startLog(7, ReferenceNo, urlApiString, imai);

      final String msg = String.format(OBMessageUtils.messageBD("SIG_ValidaCandado"),
          imai.toString());
      log.info(msg);

      Request request = new Request.Builder().url(urlApiString + "=" + imai).method("GET", null)
          .addHeader("Authorization", "Token "+tokenApiString).build();
      Response response = httpClient.newCall(request).execute();
      
      final String responseApi = response.body().string();
      log.info("**Respuesta consulta api valida candado:" + responseApi);
      
      String result = "ERROR";
      try {
    	  result = validateCodeResponse(response, responseApi);
      } catch(Exception e) {
    	  result = "ERROR";
      }
      
      newLog.endLog(objLog, responseApi, result);
      
      
      codeResponse = response.code();
      if(codeResponse == 404 || codeResponse ==401) {
    	  throw new OBException("Error de servicio"); 
      }

      

      responseJson = new JSONObject(responseApi);

      log.info("***Fin consulta api valida candado***");
      return responseJson;

    } catch (Exception e) {
      log.info("Error consulta api valida candado: " + e.getMessage());
      
      if(codeResponse == 404) {
    	  throw new OBException("URL incorrecta para NuovoPay"); 
      }
      if(codeResponse == 401) {
    	  throw new OBException("Token inválido para el servicio NuovoPay"); 
      }
      
      throw new OBException("Error al intentar valida candado: " + e.getMessage());
    }

  }

  public static JSONObject getTokenServicio() {
    JSONObject responseJson = new JSONObject();

    try {

      MediaType mediaType = MediaType.parse("text/plain");

      RequestBody body = RequestBody.create(mediaType, "");

      urlApi = getSigUrlApiEntity();
      final String urlApiString = urlApi.getTokenUrlApi();

      // https://api.cartera365.com/api-cli/v1/token
      Request request = new Request.Builder().url(urlApiString).method("POST", body)
          .addHeader("Basic", "KlNJREVTT0ZU").build();

      Response response = httpClient.newCall(request).execute();
      responseJson = new JSONObject(response.body().string());
    } catch (Exception e) {
      throw new OBException("Error get token servicio: " + e.getMessage());
    }

    return responseJson;
  }

  public static JSONObject execCrearCredito(JSONObject jsonsent) {
    JSONObject responseJson = new JSONObject();
    Shws_Log objLog = null;
    Log_Register newLog = new Log_Register();
    try {

      JSONObject jsParams = jsonsent.getJSONObject("params");
      final String terminalId = jsonsent.getString("terminalId");
      String guaranteeValue = jsParams.getString("guaranteeValue");
      CSCPS_GuaranteeType objGuarantee = OBDal.getInstance().get(CSCPS_GuaranteeType.class, guaranteeValue);
      String nameGuarantee = "";
      if (objGuarantee != null) {
	nameGuarantee = objGuarantee.getName();
      }

      OBPOSApplications obposApplications = OBDal.getInstance().get(OBPOSApplications.class,
          terminalId);
      Organization org = obposApplications.getOrganization();

      OkHttpClient.Builder builder = new OkHttpClient.Builder();
      builder.connectTimeout(120, TimeUnit.SECONDS);
      builder.readTimeout(120, TimeUnit.SECONDS);
      builder.writeTimeout(120, TimeUnit.SECONDS);
      
      User user = OBContext.getOBContext().getUser();
      
      User auxUser = OBDal.getInstance().get(User.class,user.getId());
      
      if(auxUser != null) {
    	  user =  auxUser;
      }
      
      BigDecimal sigCodigo = user.getSigCodigo();
      
      JSONObject datos_solicitud = new JSONObject(jsParams.getString("datos_solicitud"));
      datos_solicitud.put("codigo_asesor", sigCodigo);
      datos_solicitud.put("codigo_agencia", org.getSigTienda());
      datos_solicitud.put("garantia", nameGuarantee);
      
      JSONArray paramItems = (JSONArray) jsParams.get("items");
      JSONArray items = validateItems(paramItems, jsonsent.getString("ReferenceID"));
      
      validateAmountPreCalculated(jsParams.getJSONObject("factura"),  jsonsent.getString("ReferenceID"));
      
      JSONObject jsToSend = new JSONObject();
      jsToSend.put("datos_solicitud", datos_solicitud);
      jsToSend.put("politica", jsParams.get("politica"));
      jsToSend.put("factura", jsParams.get("factura"));
      jsToSend.put("operacion", jsParams.get("operacion"));
      jsToSend.put("items", items);//jsParams.get("items");
      jsToSend.put("datos_transaccion", jsParams.get("datos_transaccion"));
      
      urlApi = getSigUrlApiEntity();
      String urlApiString = urlApi.getUrlCredito();
      
      // Service 3
      String ReferenceNo = jsonsent.getString("ReferenceN0");
      objLog = newLog.startLog(3, ReferenceNo, urlApiString, jsToSend.toString());

      MediaType mediaType = MediaType.parse("application/json");


      final String msg = OBMessageUtils.messageBD("SIG_CREARCREDITO") + " Datos solicitud:"
          + datos_solicitud.toString() + " Politica: " + jsParams.get("politica").toString()
          + " Factura: " + jsParams.get("factura").toString() + " Operacion:"
          + jsParams.get("operacion").toString() + " items: " + jsParams.get("items").toString()
          + " datos transaccion: " + jsParams.get("datos_transaccion").toString();
      log.info(msg);

      RequestBody body = RequestBody.create(mediaType, jsToSend.toString());
      JSONObject tokenForServices = getTokenForServices();


      Request request = new Request.Builder().url(urlApiString).method("POST", body)
          .addHeader("Bearer", tokenForServices.getString("data"))
          .addHeader("Content-Type", "application/json").build();
      Response response = httpClient.newCall(request).execute();

      final String responseApi = response.body().string();
      log.info("**Respuesta consulta api crear credito:" + responseApi);
      
      String result = "ERROR";
      try {
    	  result = validateCodeResponse(response, responseApi);
      } catch(Exception e) {
    	  result = "ERROR";
      }
      
      newLog.endLog(objLog, responseApi, result);
      
      responseJson = new JSONObject(responseApi);
      log.info("***Fin consulta api crear credito***");
      
      
      if(result.equals("ERROR")) {
    	  responseJson = retriveJSON(ReferenceNo, "ingreso-credito", "OK", responseJson);
      }
      

    } catch (Exception e) {
	if (objLog != null) {
		String responseError = "msg: " + e.getMessage() + " cause: " + e.getCause();
		newLog.endLog(objLog, responseError, "ERROR");
	}
	log.info("Error consulta api crear credito: " + e.getMessage());
	throw new OBException("Error al intentar crear crédito " + e.getMessage());
    }

    return responseJson;

  }
  
  public static JSONArray validateItems(JSONArray items, String ReferenceID) throws JSONException {
	  for (int i = 0; i < items.length(); i++) {
			JSONObject JSONproduct = items.getJSONObject(i);
			String keyProd = JSONproduct.getString("codigo_barras");
			String idProd = JSONproduct.getString("id");
			String originImei = JSONproduct.getString("imei");
			String originNumero_celular = JSONproduct.getString("numero_celular");
			
			if(keyProd != null && !keyProd.equals("") && JSONproduct.getString("garantia").equals("SI") && originImei.equals("") && originNumero_celular.equals("")) {
			    	Order objOrder = OBDal.getInstance().get(Order.class, ReferenceID);
			    	List<OrderLine> lines = objOrder.getOrderLineList();
					for(OrderLine line : lines) {
						if(keyProd.equals(line.getProduct().getSearchKey())) {
							String imei = (line.getSigEmei() != null)? line.getSigEmei(): "";
							String tlf = (line.getSigTelefono() != null)? line.getSigTelefono() : "";
							
							if(imei != null && !imei.equals("")) {
								JSONproduct.put("imei", imei);
							}else {
								log.info("No se ha encontrado un IMEI válido. Por favor, elimine la línea y vuelva a agregar el producto para registrar el IMEI.");
							      throw new OBException("No se ha encontrado un IMEI válido. Por favor, elimine la línea y vuelva a agregar el producto para registrar el IMEI.");
							}
							
							if(tlf != null && !tlf.equals("")) {
								JSONproduct.put("numero_celular", tlf);
							}else {
								log.info("No se ha encontrado un Teléfono válido. Por favor, elimine la línea y vuelva a agregar el producto para registrar el Tlf.");
							      throw new OBException("No se ha encontrado un Teléfono válido. Por favor, elimine la línea y vuelva a agregar el producto para registrar el Tlf.");
							}
							
						}
					}
			}
			
			//  Informacion adicional del producto
			if(idProd != null && !idProd.equals("") ) {
				Product objProduct = OBDal.getInstance().get(Product.class, idProd);
				if(objProduct != null) {
					String prodRam = (objProduct.getSinimeiRam() != null) ? objProduct.getSinimeiRam().getName() : "S/N";
					String prodRom = (objProduct.getSinimeiRom() != null) ? objProduct.getSinimeiRom().getName() : "S/N";
					String prodBrand = (objProduct.getBrand() != null) ? objProduct.getBrand().getName() : "S/N";
					String prodModel = (objProduct.getSsfiModelProd() != null) ? objProduct.getSsfiModelProd().getName(): "S/N";
					String prodModel2 = (objProduct.getSinimeiCommercialModel() != null)? objProduct.getSinimeiCommercialModel().getName(): "S/N";
					String prodColor = (objProduct.getSinimeiColor() != null) ? objProduct.getSinimeiColor().getName(): "S/N";
					String prodCategoryName = (objProduct.getProductCategory() != null)? objProduct.getProductCategory().getName(): "S/N";
					
					JSONproduct.put("memoria_ram", prodRam);
					JSONproduct.put("memoria_rom", prodRom);
					JSONproduct.put("marca", prodBrand);
					JSONproduct.put("modelo", prodModel+" - "+prodModel2);
					JSONproduct.put("color", prodColor);
					JSONproduct.put("generico", prodCategoryName);
					JSONproduct.remove("id");
				}
			}
			
	  }
	  
	  return items;
  }

  public static JSONObject validaDocElectronico(final String operacionCredito, final String ReferenceN0) {
    JSONObject responseJson = new JSONObject();
    try {

      urlApi = getSigUrlApiEntity();
      final String urlApiString = urlApi.getURLValidaDocElectrnico();

      final String msg = OBMessageUtils.messageBD("SIG_VlidadocElectronico") + "Operacion credito: "
          + operacionCredito.toString();
      log.info(msg);
      
      // Service 4 
      String ReferenceNo = ReferenceN0;
      Log_Register newLog = new Log_Register();
      Shws_Log objLog = newLog.startLog(4, ReferenceNo, urlApiString, operacionCredito);
      
      Request request = new Request.Builder().url(urlApiString + operacionCredito)
          .method("GET", null).addHeader("Cookie", "ci_session=0u171hu016rd8n1dm0knilp8lah1s9kl")
          .build();
      Response response = httpClient.newCall(request).execute();

      final String responseApi = response.body().string();
      //log.info("**Respuesta consulta api valida documento electronico: " + responseApi);
      
      String result = "ERROR";
      try {
    	  result = validateCodeResponse(response, responseApi);
      } catch(Exception e) {
    	  result = "ERROR";
      }
      
      newLog.endLog(objLog, responseApi, result);

      responseJson = new JSONObject(responseApi);
      log.info("****Fin consulta api valida documento electronico***");

      return responseJson;
    } catch (Exception e) {
      log.info("Error consulta api valida documento electronico: " + e.getMessage());
      throw new OBException("Error al validar documento electronico: " + e.getMessage());
    }
  }

  public static JSONObject validaDocManuales(final String operacionCredito, final String ReferenceN0) {
    JSONObject responseJson = new JSONObject();
    try {

      urlApi = getSigUrlApiEntity();
      final String urlApiString = urlApi.getURLValidaDocManuales();
    	
      // Service 8
	  String ReferenceNo = ReferenceN0;
	  Log_Register newLog = new Log_Register();
	  Shws_Log objLog = newLog.startLog(8, ReferenceNo, urlApiString, operacionCredito);


      final String msg = OBMessageUtils.messageBD("SIG_ValidaDocManuales")
          + operacionCredito.toString();
      log.info(msg);

      Request request = new Request.Builder().url(urlApiString + operacionCredito)
          .method("GET", null).addHeader("Cookie", "ci_session=0u171hu016rd8n1dm0knilp8lah1s9kl")
          .build();
      Response response = httpClient.newCall(request).execute();
      final String responseApi = response.body().string();
      log.info("Respuesta consulta api valida documentos manuales:" + responseApi);
      
      
      String result = "ERROR";
      try {
    	  result = validateCodeResponse(response, responseApi);
      } catch(Exception e) {
    	  result = "ERROR";
      }
      
      newLog.endLog(objLog, responseApi, result);
      
      responseJson = new JSONObject(responseApi);

      log.info("Fin consulta api valida documentos manuales:" + responseApi);

      return responseJson;
    } catch (Exception e) {
      log.info("Error consulta api valida documentos manuales: " + e.getMessage());
      throw new OBException("Error al validar documento manuales: " + e.getMessage());
    }
  }

  private static JSONObject getTokenForServices() {
    JSONObject responseJson = new JSONObject();
    try {
     
      MediaType mediaType = MediaType.parse("text/plain");
      RequestBody body = RequestBody.create(mediaType, "");

      urlApi = getSigUrlApiEntity();
      final String urlApiString = urlApi.getTokenUrlApi();
      final String tokenApiString = urlApi.getCscpsTokenCcredit();

      // https://api.cartera365.com/api-cli/v1/token
      Request request = new Request.Builder().url(urlApiString).method("POST", body)
          .addHeader("Basic", tokenApiString).build();

      Response response = httpClient.newCall(request).execute();
      responseJson = new JSONObject(response.body().string());
    } catch (Exception e) {
      throw new OBException("Error to try token for services");
    }

    return responseJson;
  }

  public static JSONObject confirmarCredito(final Invoice inv) {
    JSONObject responseJson = new JSONObject();
    JSONObject jsonConfCredito = new JSONObject();
    JSONObject datos_solicitud = new JSONObject();
    JSONObject factura = new JSONObject();
    JSONObject datos_transaccion = new JSONObject();
    Log_Register newLog = new Log_Register();
    Shws_Log objLog = null;
    
    try {
      Date fecha2 = new Date();
      Organization org = inv.getOrganization();
      datos_solicitud.put("cod_operacio_credito", inv.getSigOpcredito());
      datos_solicitud.put("codigo_agencia", org.getSigTienda());

      Order order = inv.getSalesOrder();
      SigMasterData sigMasterData = order.getSigMasterDataList().size() > 0
          ? order.getSigMasterDataList().get(0)
          : null;

      if (sigMasterData != null) {

        String hqlQuery = "select trim(sig.cedula) from Sig_MasterData sig where sig.id = '"
            + sigMasterData.getId() + "'";
        final Session session = OBDal.getInstance().getSession();
        final Query query = session.createQuery(hqlQuery);
        query.setMaxResults(1);
        final String cedula = (String) query.uniqueResult();
        datos_solicitud.put("clie_identificacion", cedula.trim());
      }

      factura.put("monto_factura_con_iva", inv.getGrandTotalAmount());
      factura.put("monto_factura_sin_iva", inv.getSummedLineAmount());
      factura.put("numero_factura", inv.getDocumentNo());
      factura.put("numero_autorizacion_sri", inv.getEeiCodigo());

      urlApi = getSigUrlApiEntity();

      datos_transaccion.put("usuario_facturador", urlApi.getUsuarioGenerador());
      datos_transaccion.put("fecha_hora_registro", formatter.format(fecha2));
      datos_transaccion.put("usuario_generador", urlApi.getUsuarioGenerador());

      jsonConfCredito.put("datos_solicitud", datos_solicitud);
      jsonConfCredito.put("factura", factura);
      jsonConfCredito.put("datos_transaccion", datos_transaccion);

      final String msg = OBMessageUtils.messageBD("SIG_ConfirmarCredito") + " Datos solicitud: "
          + datos_solicitud + " Factura:" + factura + " Datos transaccion:" + datos_transaccion;

      log.info(msg);

      JSONObject tokenForServices = getTokenForServices();
      final String urlApiString = urlApi.getConfirmarCreditoApi();
      
      // Service 5
      String ReferenceNo = inv.getDocumentNo()+"";
      Order objOrder = inv.getSalesOrder();
      if(objOrder != null) {
    	  String auxNumDoc = objOrder.getDocumentNo();
    	  ReferenceNo = (auxNumDoc != null && !auxNumDoc.equals(""))?auxNumDoc:ReferenceNo;
      }
      
      objLog = newLog.startLog(5, ReferenceNo, urlApiString, jsonConfCredito.toString());
      
      MediaType mediaType = MediaType.parse("application/json");
      RequestBody body = RequestBody.create(mediaType, jsonConfCredito.toString());

      Request request = new Request.Builder().url(urlApiString).method("POST", body)
          .addHeader("Bearer", tokenForServices.getString("data"))
          .addHeader("Content-Type", "application/json").build();
      Response response = httpClient.newCall(request).execute();
      final String responseApi = response.body().string();
      log.info("**Respuesta consulta api confirmar credito:" + responseApi);
      
      
      String result = "ERROR";
      try {
    	  result = validateCodeResponse(response, responseApi);
      } catch(Exception e) {
    	  result = "ERROR";
      }
      
      newLog.endLog(objLog, responseApi, result);
      
      responseJson = new JSONObject(responseApi);
      log.info("***Fin consulta api confirmar credito***");

    } catch (Exception e) {
    	if(objLog != null) {
    		String responseError = "msg: "+e.getMessage()+" cause: "+e.getCause();
    		newLog.endLog(objLog, responseError, "ERROR");
    	}
      throw new OBException("Error al intentar confirmar credito: " + jsonConfCredito.toString());
    }
    return responseJson;
  }

  private static SigUrlApi getSigUrlApiEntity() {
	  urlApi = null;
    if (urlApi == null) {
      StringBuilder whereClause = new StringBuilder();
      whereClause.append(" as urlapi");
      final OBQuery<SigUrlApi> obq = OBDal.getInstance().createQuery(SigUrlApi.class,
          whereClause.toString());
      obq.setMaxResult(1);
      urlApi = obq.uniqueResult();
    }

    return urlApi;
  }

  public static BusinessPartner createBusinessPartner(OBPOSApplications obposApplications,
      JSONObject newClient) {
    final BusinessPartner defaultCustomer = obposApplications.getDefaultCustomer();
    BusinessPartner newBusinessPartner = null;
    try {

      final OBCriteria<BusinessPartner> bpCriteria = OBDal.getInstance()
          .createCriteria(BusinessPartner.class);
      bpCriteria.setFilterOnActive(false);
      bpCriteria.setFilterOnReadableOrganization(false);
      bpCriteria.add(Restrictions.eq("searchKey", newClient.getString("id")));
      bpCriteria.setMaxResults(1);
      newBusinessPartner = (BusinessPartner) bpCriteria.uniqueResult();

      if (newBusinessPartner == null) {
        newBusinessPartner = (BusinessPartner) DalUtil.copy(defaultCustomer, false);
        newBusinessPartner.setNewOBObject(true);
        final String newBusinessPartnerId = SequenceIdData.getUUID();
        newBusinessPartner.setId(newBusinessPartnerId);
        newBusinessPartner.setCreationDate(new Date());

        String nameComercial = newClient.getString("nombres") + "  "
            + newClient.getString("apellidos");
        newBusinessPartner.setName(nameComercial);
        newBusinessPartner.setName2(nameComercial);

        newBusinessPartner.setCreditUsed(BigDecimal.ZERO);
        newBusinessPartner.setSearchKey(newClient.getString("id"));
        newBusinessPartner.setCurrency(defaultCustomer.getCurrency());
        newBusinessPartner.setPriceList(defaultCustomer.getPriceList());
        newBusinessPartner.setTaxID(newClient.getString("id"));
        newBusinessPartner.setEEIEmail(newClient.getString("email"));

        final Organization org = obposApplications.getOrganization();
        final String taxIdType = org.getSigTaxidtype();
        newBusinessPartner.setSswhTaxidtype(taxIdType);
        OBDal.getInstance().save(newBusinessPartner);

        List<Location> locList = defaultCustomer.getBusinessPartnerLocationList();
        Location locationBp = locList.get(0);
        Location newLocationBp = (Location) DalUtil.copy(locationBp, false);

        org.openbravo.model.common.geography.Location locationAddress = locationBp
            .getLocationAddress();
        org.openbravo.model.common.geography.Location newlocationAddress = (org.openbravo.model.common.geography.Location) DalUtil
            .copy(locationAddress, false);
        newlocationAddress.setNewOBObject(true);
        newlocationAddress.setId(SequenceIdData.getUUID());
        newlocationAddress.setAddressLine1(newClient.getString("direccion1"));
        newlocationAddress.setAddressLine2(newClient.getString("direccion2"));
        newlocationAddress.setCityName(newClient.getString("pais"));
        newlocationAddress.setCountry(locationAddress.getCountry());
        newlocationAddress.setRegion(locationAddress.getRegion());
        newlocationAddress.setOrganization(newBusinessPartner.getOrganization());
        OBDal.getInstance().save(newlocationAddress);
        OBDal.getInstance().flush();

        newLocationBp.setNewOBObject(true);
        newLocationBp.setId(SequenceIdData.getUUID());
        newLocationBp.setLocationAddress(newlocationAddress);
        newLocationBp.setPhone(newClient.getString("telefono1"));
        newLocationBp.setAlternativePhone(newClient.getString("telefono2"));
        newLocationBp.setBusinessPartner(newBusinessPartner);
        newLocationBp.setOrganization(newBusinessPartner.getOrganization());
        OBDal.getInstance().save(newLocationBp);
        newBusinessPartner.getBusinessPartnerLocationList().add(newLocationBp);
        OBDal.getInstance().flush();
        newBusinessPartner.setNewOBObject(true);
      }
    } catch (Exception e) {
      throw new OBException(e.getMessage());
    }
    return newBusinessPartner;
  }
  
  public static String validateCodeResponse(Response response, String responseApi) throws Exception{
	  String result = "OK";
      int codeResponse = response.code();
      if(codeResponse == 404 || codeResponse == 401 || codeResponse == 500 ||  codeResponse ==400) {
    	  result = "ERROR";
      }else {
    	  JSONObject objResponse = new JSONObject(responseApi); 
    	  if(objResponse.has("codigo")) {
    		  codeResponse = objResponse.getInt("codigo");
        	  if(codeResponse == 404 || codeResponse == 401 || codeResponse == 500 ||  codeResponse ==400) {
        		  result = "ERROR";
        	  }
    	  }
      }
      
      return result;
  }
  
  public static JSONObject retriveJSON(String referenceNo, String endpoint, String result, JSONObject apiresponse) {
	try {
		log.info("*** Recuperando Log para "+referenceNo+" ***");
		OBCriteria<Shws_Log> queryJson= OBDal.getInstance().createCriteria(Shws_Log.class);
		queryJson.add(Restrictions.eq(Shws_Log.PROPERTY_REFERENCENO, referenceNo));
		queryJson.add(Restrictions.eq(Shws_Log.PROPERTY_RESULT, result));
		queryJson.add(Restrictions.eq(Shws_Log.PROPERTY_ENDPOINT, endpoint));
		queryJson.addOrder(org.hibernate.criterion.Order.desc(Shws_Log.PROPERTY_CREATIONDATE));
		List<Shws_Log> listJson = queryJson.list();
		
		if(listJson.size() > 0 ) {
				String retriveResponse = listJson.get(0).getJsonResponse();
				if(retriveResponse != null && !retriveResponse.equals("")) {
					apiresponse = new JSONObject(retriveResponse);
				}
		}
		
	}catch(Exception e) {
		log.info("*** Error recuperando Log para "+referenceNo+" "+e.getMessage()+" ***");
	}
	
	  
	return apiresponse;
  }
  
  public static void  validateAmountPreCalculated(JSONObject factura, String ReferenceID) throws JSONException{
	  Order objOrder = OBDal.getInstance().get(Order.class, ReferenceID);
	  if(objOrder == null) {
		  throw new OBException("Verificar la existencia del pedido "+ReferenceID);
	  }
	  
	  String monto_factura_con_iva = factura.getString("monto_factura_con_iva");
	  if(monto_factura_con_iva == null || monto_factura_con_iva.equals("")) {
		  throw new OBException("Verificar la existencia de monto_factura_con_iva");
	  }
	  
	 BigDecimal amountOrder =  objOrder.getShpcAmountprecalculated();
	 BigDecimal amountJSONOrder =  new BigDecimal(monto_factura_con_iva);
	 
	 if (amountOrder.compareTo(amountJSONOrder) != 0) {
		 throw new OBException("Los montos a enviar no concuerdan con los montos resultantes de la calculadora");
	 }
	  
  }

}
