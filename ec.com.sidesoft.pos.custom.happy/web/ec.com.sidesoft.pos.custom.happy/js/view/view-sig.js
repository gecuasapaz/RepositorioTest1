(function () {
  OB.SIG = OB.SIG || {};
  OB.SIG.Utils = OB.SIG.Utils || {};  

  OB.SIG.Utils.PaymentType = [];
  OB.SIG.Utils.PaymentType = [{id:'SIG_TarjetaDebito', name: 'Tarjeta Debito'}, 
                              {id:'SIG_PlanPagos', name: 'Plan Pagos'},
                              {id:'SIG_TarjetaIntereses', name: 'Tarjeta con intereses'}, 
                              {id:'SIG_TarjetaSinIntereses', name: 'Tarjeta sin intereses'},
                              {id:'SIG_Efectivo', name: 'Efectivo'}]

  OB.SIG.Utils.consultaCedula = function (dataparams, callback, callbackError){
    var process = new OB.DS.Process('ec.com.sidesoft.pos.custom.happy.ConsultaCedula');
    process.exec({
      cedula:dataparams,
      ReferenceN0: OB.MobileApp.model.receipt.get('documentNo'),
      terminalId: OB.MobileApp.model.get('terminal').id
      }, function (data) {
      if (data && data.exception) {
        callbackError(data);
      } else if (data) {
        if (data.bPartner.bpIsNew === true){
          var datasources = [OB.Model.BusinessPartner, OB.Model.BPLocation];
          OB.Dal.loadModels(false, datasources, null, true);  
        }
        callback(data);
      }
    }, callbackError);
  }
  
  OB.SIG.Utils.simulacion = function (dataparams, callback, callbackError){
	  dataparams.ReferenceN0 = OB.MobileApp.model.receipt.get('documentNo');
	  dataparams.guaranteeValue = OB.MobileApp.model.receipt.get('autorizacion').guaranteeValue  || ''; 
      var process = new OB.DS.Process('ec.com.sidesoft.pos.custom.happy.SimulacionSigma');
      process.exec(dataparams, function (data) {
        if (data && data.exception) {
          callbackError(data);
        } else if (data) {
          callback(data);
        }
      }, callbackError);
    }


    OB.SIG.Utils.validaCandado = function (dataparams, callback, callbackError){
	  dataparams.ReferenceN0 = OB.MobileApp.model.receipt.get('documentNo');
      var process = new OB.DS.Process('ec.com.sidesoft.pos.custom.happy.ValidaCandado');
      process.exec(dataparams, function (data) {
	//console.log(dataparams);
	//console.log(data);
        if (data && data.exception) {
          callbackError(data);
        } else if (data) {
		  OB.SIG.Utils.validaCandadoParams = dataparams;//save parameters and create new property
		  OB.SIG.Utils.validaCandadoResult = data;//save result and create new property
          callback(data);
        }
      }, callbackError);
    }

    
    OB.SIG.Utils.validaDocElectronico = function (dataparams, callback, callbackError){
	  dataparams.ReferenceN0 = OB.MobileApp.model.receipt.get('documentNo');
      var process = new OB.DS.Process('ec.com.sidesoft.pos.custom.happy.ValidaDocElectronico');
      process.exec(dataparams, function (data) {
        if (data && data.exception) {
          callbackError(data);
        } else if (data) {
          callback(data);
        }
      }, callbackError);
    }

    

    OB.SIG.Utils.validaDocManuales = function (dataparams, callback, callbackError){
      var process = new OB.DS.Process('ec.com.sidesoft.pos.custom.happy.ValidaDocManuales');
      dataparams.ReferenceN0 = OB.MobileApp.model.receipt.get('documentNo');
      process.exec(dataparams, function (data) {
        if (data && data.exception) {
          callbackError(data);
        } else if (data) {
          callback(data);
        }
      }, callbackError);
    }


    OB.SIG.Utils.ingresoCredito = function (dataparams, callback, callbackError){
      dataparams.guaranteeValue = OB.MobileApp.model.receipt.get('autorizacion').guaranteeValue || ''; 
      var process = new OB.DS.Process('ec.com.sidesoft.pos.custom.happy.CrearCredito');
      process.exec({
        params:dataparams,
        ReferenceN0: OB.MobileApp.model.receipt.get('documentNo'),
        terminalId: OB.MobileApp.model.get('terminal').id,
        ReferenceID: OB.MobileApp.model.receipt.get('id')
      }, function (data) {
        if (data && data.exception) {
          callbackError(data);
        } else if (data) {
          callback(data);
        }
      }, callbackError);
    }
    
    OB.SIG.Utils.getDatosSolicitud = function (){

      var receipt = OB.MobileApp.model.receipt;
      var numero_autorizacion = receipt.get('autorizacion').numero_autorizacion;
      var cedula = receipt.get('autorizacion').cedula;
      var codigo_biometrico = receipt.get('autorizacion').codigo_biometrico;
      var datos_solicitud = {
        "numero_solicitud_credito": numero_autorizacion,
        "codigo_agencia": 3,
        "clie_identificacion": cedula,
        "codigo_asesor": 6,
        "codigo_biometrico": codigo_biometrico
     }
     return datos_solicitud;
    }

    OB.SIG.Utils.getDatosPolitica = function (){

       var receipt = OB.MobileApp.model.receipt;
       var numero_autorizacion = receipt.get('autorizacion').numero_autorizacion;
       var cupo_maximo = receipt.get('autorizacion').cupo_maximo;
       var plazo_maximo = receipt.get('autorizacion').plazo_maximo;
       var entrada = receipt.get('autorizacion').entrada;
       var cupo_disponible = receipt.get('autorizacion').cupo_disponible;
        var politica = {
          "numero_autorizacion": numero_autorizacion,
          "cupo_maximo": cupo_maximo,
          "plazo_maximo": plazo_maximo,
          "porcentaje_entrada_minima": 30.00, 
          "entrada": entrada,
          "cupo_disponible": cupo_disponible
      }
      return politica;
    }  

    OB.SIG.Utils.getDatosFactura = function (){

      var receipt = OB.MobileApp.model.receipt;

      var factura = {
        "monto_factura_con_iva":receipt.get('gross'),
        "monto_factura_sin_iva": receipt.get('net'),
        "numero_factura": null,
        "numero_autorizacion_sri": null
    }
    return factura;
   } 


   OB.SIG.Utils.getDatosOperacion = function (){

      var receipt = OB.MobileApp.model.receipt;
      var sigSimulation = receipt.get('sigSimulation');
      var autorizacion = receipt.get('autorizacion');
      
      var capital_total = OB.DEC.sub(new BigDecimal(receipt.get('gross').toString()), new BigDecimal(autorizacion.entrada2.toString()));
      var total_cuota = OB.DEC.add(OB.DEC.add(new BigDecimal(sigSimulation.capital), new BigDecimal(sigSimulation.asistencia)), new BigDecimal(sigSimulation.servicio));
      
      var current_date = new Date();
      current_date.setDate(current_date.getDate() + 7);    
      var operacion =  {
        "plazo": sigSimulation.plazo,
        "capital_total": capital_total.toString(),
        "monto_total": sigSimulation.monto,//sigSimulation.cuota,
        "capital_cuota": sigSimulation.capital,
        "asistencia_cuota": sigSimulation.asistencia,
        "servicio_cuota": sigSimulation.servicio,
        "total_cuota": total_cuota,
        "fecha_primer_pago": OB.SIG.Utils.formatDate(current_date), 
        "valor_entrada": autorizacion.entrada2
    }
    
	receipt.set('sendSigCartera', operacion);
    
    return operacion;
  } 

  OB.SIG.Utils.formatDate = function(date){
    let formatted_date =date.getFullYear()  + "-" + (date.getMonth() + 1)  + "-" + date.getDate();
     return formatted_date;
    }
    
  OB.SIG.Utils.formatDate2 = function(date){
    let year = date.getFullYear();
    let month = (date.getMonth() + 1).toString().padStart(2, '0');
    let day = date.getDate().toString().padStart(2, '0');
    let hours = date.getHours().toString().padStart(2, '0');
    let minutes = date.getMinutes().toString().padStart(2, '0');
    let seconds = date.getSeconds().toString().padStart(2, '0');
    let formatted_date = `${year}-${month}-${day}`;
    let formatted_time = `${hours}:${minutes}:${seconds}`;
    return formatted_date+" "+formatted_time;
    }

  OB.SIG.Utils.getDatosItems = function (){

    var items = [];    
    
    var lines = OB.MobileApp.model.receipt.get('lines');  
    _.forEach(lines.models, function (element) {
      var product = element.get('product');
	  //console.log("PRODUCTO", product);
      var imei = '';
      var prodCellphoneNumber = '';
      
      var selectedPaymentType =
         OB.MobileApp.view.$.containerWindow.$.pointOfSale.$.multiColumn.$.leftPanel.$.receiptview.$.receiptheader.$.receiptLabels.$.paymentTypeSigma.$.renderComboPaymentType.getValue();
      
     // if(selectedPaymentType === 'SIG_PlanPagos'){
		 /*if(OB.SIG.Utils.validaCandadoParams.emai){
			prodCellphoneNumber = OB.SIG.Utils.validaCandadoParams.telefono;
	        imei = OB.SIG.Utils.validaCandadoParams.emai;
	     }else{
	        imei = product.get('garantizable') === true? product.get('imei'):'123456789';
	              imei = imei === undefined? '123456789':imei;
	     }*/
	     
	     var isGarantizable = product.get('garantizable');
	     
	     if(isGarantizable){
			 var elementImei = element.get('Imei');
		     if(elementImei && (elementImei != "") && (!OB.UTIL.isNullOrUndefined(elementImei)) ){
		         imei = elementImei;
			 }
			 
			 var elementTlf = element.get('telefono');
			 if(elementTlf && (elementTlf != "") && (!OB.UTIL.isNullOrUndefined(elementTlf))){
				 prodCellphoneNumber = elementTlf;
			 }
				
			 var paramImei = OB.SIG.Utils && OB.SIG.Utils.validaCandadoParams && OB.SIG.Utils.validaCandadoParams.emai ? OB.SIG.Utils.validaCandadoParams.emai : null;
		     if(paramImei && (paramImei != "") && (!OB.UTIL.isNullOrUndefined(paramImei)) && imei == null){
				imei = paramImei;
			 }
			 
			 var paramTlf = OB.SIG.Utils && OB.SIG.Utils.validaCandadoParams && OB.SIG.Utils.validaCandadoParams.telefono ? OB.SIG.Utils.validaCandadoParams.telefono : null;
		     if(paramTlf && (paramTlf != "") && (!OB.UTIL.isNullOrUndefined(paramTlf)) && prodCellphoneNumber == null){
				prodCellphoneNumber = paramTlf;
		     }
		     
		 }
	     
	     
      //}
		
      var iddevice = product.get('garantizable') === true? '2473962':'';

      var financiado = product.get('financiable') ===true? 'SI':'NO';
      var garantia = product.get('garantizable') ===true? 'SI':'NO';

      var uPCEAN = (OB.UTIL.isNullOrUndefined(product.get('uPCEAN'))  || _.isEmpty(product.get('uPCEAN')))? "05050555555": product.get('uPCEAN');
      var marca = (OB.UTIL.isNullOrUndefined(product.get('brand'))  || _.isEmpty(product.get('brand')))? "Sansum": product.get('brand');
      var modelo = (OB.UTIL.isNullOrUndefined(product.get('ss_model'))  || _.isEmpty(product.get('ss_model')))? "tr1234": product.get('ss_model');
	  
      var barCode = (OB.UTIL.isNullOrUndefined(product.get('searchkey'))  || _.isEmpty(product.get('searchkey')))? "05050555555": product.get('searchkey');     
      var prodBrand = (OB.UTIL.isNullOrUndefined(product.get('prodBrand'))  || _.isEmpty(product.get('prodBrand')))? "S/N": product.get('prodBrand');       
      var prodModel = (OB.UTIL.isNullOrUndefined(product.get('prodModel'))  || _.isEmpty(product.get('prodModel')))? "S/N": product.get('prodModel');       
      var prodModel2 = (OB.UTIL.isNullOrUndefined(product.get('prodModel2'))  || _.isEmpty(product.get('prodModel2')))? "S/N": product.get('prodModel2');       
      var prodColor = (OB.UTIL.isNullOrUndefined(product.get('prodColor'))  || _.isEmpty(product.get('prodColor')))? "S/N": product.get('prodColor');       
      var prodRam = (OB.UTIL.isNullOrUndefined(product.get('prodRam'))  || _.isEmpty(product.get('prodRam')))? "S/N": product.get('prodRam');       
      var prodRom = (OB.UTIL.isNullOrUndefined(product.get('prodRom'))  || _.isEmpty(product.get('prodRom')))? "S/N": product.get('prodRom');            
      var prodTypePadlock = (OB.UTIL.isNullOrUndefined(product.get('prodTypePadlock'))  || _.isEmpty(product.get('prodTypePadlock')))? "S/N": product.get('prodTypePadlock');   
      var prodIsGeneric = (product.get('prodIsGeneric') === true) ? "Si" : ((product.get('prodIsGeneric') === false) ? "No" : "S/N");
      var prodCategoryName = (OB.UTIL.isNullOrUndefined(product.get('prodCategoryName'))  || _.isEmpty(product.get('prodCategoryName')))? "S/N": product.get('prodCategoryName');

	  if(prodTypePadlock === 'SIG_T'){
		prodTypePadlock = 'Trustonic';
	  } else if(prodTypePadlock === 'SIG_N'){
		prodTypePadlock = 'Nuovopay';
	  }
	  
	  var discountedNetPrice = element.get('discountedNetPrice') || 0;
	  var linerate = element.get('linerate') || 0;
	  var precioUnitarioConIva = discountedNetPrice * linerate;
	  precioUnitarioConIva = parseFloat(precioUnitarioConIva.toFixed(2));

      var item = {
	    "id": product.get('id'),
            "codigo_barras": barCode,
            "nombre": product.get('_identifier'),
            "cantidad": element.get('qty'),
            "precio_unitario_sin_iva": element.get('discountedNetPrice'),
            "precio_unitario_con_iva": precioUnitarioConIva,
            "marca": prodBrand,
            "modelo": prodModel+" - "+prodModel2,
            "color": prodColor,
            "memoria_rom": prodRom,
            "memoria_ram": prodRam,
            "imei": imei,
            "iddevice": iddevice,
            "numero_celular": prodCellphoneNumber,
            "generico": prodCategoryName,//prodIsGeneric,
            "garantia": garantia,
            "financiado": financiado, //financiable
            "tipo_candado": prodTypePadlock

     };

    items.push(item);

    });
  return items;
}


OB.SIG.Utils.getDatosTransaccion = function (){
  var current_date = new Date();

    var datos_transaccion = {
      "usuario_facturador": "ADMINISTRADOR", 
      "fecha_hora_registro": OB.SIG.Utils.formatDate2(current_date), 
      "usuario_generador": "*Sidesoft" 
    }
    return datos_transaccion;
}  

OB.SIG.Utils.exeIngresoCredito = function(callback, callbackError){
  var datosIngresoCredito = OB.SIG.Utils.getDatosIngresoCredito();
  OB.SIG.Utils.ingresoCredito(datosIngresoCredito, callback, callbackError);
}

OB.SIG.Utils.getDatosIngresoCredito = function (){
  var ingresoCredito = {};

  var datos_solicitud = OB.SIG.Utils.getDatosSolicitud();
  ingresoCredito.datos_solicitud = datos_solicitud;

  var politica = OB.SIG.Utils.getDatosPolitica();
  ingresoCredito.politica = politica;

  var factura = OB.SIG.Utils.getDatosFactura();
  ingresoCredito.factura = factura;

  var operacion = OB.SIG.Utils.getDatosOperacion();
  ingresoCredito.operacion = operacion;

  var items = OB.SIG.Utils.getDatosItems();
  ingresoCredito.items = items;

  var datos_transaccion = OB.SIG.Utils.getDatosTransaccion();
  ingresoCredito.datos_transaccion = datos_transaccion;

	
  var update_Order = new OB.DS.Process('ec.com.sidesoft.retail.custompos.UpdateOrder');
  update_Order.exec({
	  data: {
	    receipt: OB.MobileApp.model.receipt,
		terminalId: OB.MobileApp.model.get('terminal').id
	  }
	}, function (data) {
		console.log("Updated Order", data);
	}, function (error) {
		console.error("Failed to update imei at order.", error);
	});
  

  return ingresoCredito;
   
}



    OB.UTIL.HookManager.registerHook('OBRETUR_ReturnFromOrig', function (args, callbacks) {
      var order = args.order; 
      var autorizacion = {
          'cupo_maximo': OB.UTIL.isNullOrUndefined(order.cupo_maximo)? 0 : order.cupo_maximo,
           'plazo_maximo': OB.UTIL.isNullOrUndefined(order.plazo_maximo)? 0 :order.plazo_maximo,
           'entrada': OB.UTIL.isNullOrUndefined(order.entrada)? 0 : order.entrada,
           'cupo_disponible': OB.UTIL.isNullOrUndefined(order.cupo_disponible)? 0 : order.cupo_disponible,
           'tipo_entrada': OB.UTIL.isNullOrUndefined(order.tipo_entrada)? 0 : order.tipo_entrada,
           'paymentTypeValue':OB.UTIL.isNullOrUndefined(order.paymentTypeValue)? 0 : order.paymentTypeValue,
           'numero_autorizacion':OB.UTIL.isNullOrUndefined(order.numero_autorizacion)? 0 : order.numero_autorizacion,
           'canalValue':OB.UTIL.isNullOrUndefined(order.canalValueOrigin)? 0 : order.canalValueOrigin,
           'cedula':OB.UTIL.isNullOrUndefined(order.cedula)? 0 : order.cedula,
           'codigo_biometrico':OB.UTIL.isNullOrUndefined(order.codigo_biometrico)? 0 : order.codigo_biometrico,
           'validar_candado':OB.UTIL.isNullOrUndefined(order.validar_candado)? 0 : order.validar_candado,
           'entrada2':OB.UTIL.isNullOrUndefined(order.entrada2)? 0 : Number(order.entrada2),
	   'guaranteeValue':OB.UTIL.isNullOrUndefined(order.GuaranteeValueOrigin)? 0 : order.GuaranteeValueOrigin,
      }

      order.autorizacion = autorizacion;

      var sigSimulation ={
        plazo:order.plazo,
        capital:order.capital,
        servicio:order.servicio,
        asistencia:order.asistencia,
        cuota:order.cuota,
        monto:order.monto,
        valor_entrada: Number(order.valor_entrada),
        processed:"Y",
        'checked':true
       };      
      order.sigSimulation = sigSimulation;
      OB.UTIL.HookManager.callbackExecutor(args, callbacks);
});


}());
