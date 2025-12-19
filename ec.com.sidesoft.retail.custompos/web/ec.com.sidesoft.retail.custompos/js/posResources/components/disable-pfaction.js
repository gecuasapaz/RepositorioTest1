(function () {
	
	function funcCustomValidateOrderModified(){
	var keyboard = OB.MobileApp.view.$.containerWindow.$.pointOfSale.$.multiColumn.$.rightPanel.$.keyboard;
	if (keyboard) {
		
		var btnQty = keyboard.$.btnQty.$.button	
		var minusBtn = keyboard.$.minusBtn.$.button;
		var plusBtn = keyboard.$.plusBtn.$.button;
	    var btnDiscount = keyboard.$.btnDiscount.$.button;
		var btnUPC = keyboard.$.toolbarcontainer.$.control.$.btnSide.$.code.$.button;
		
		var rightPanel = OB.MobileApp.view.$.containerWindow.$.pointOfSale.$.multiColumn.$.rightPanel;
		var actionsButtons = rightPanel.$.toolbarpane.$.edit.$.editTabContent.$.actionButtonsContainer;
		var btnDelete = actionsButtons.$.deleteLine;
		
		keyboard.$.btnQty.$.button.tap= _.wrap(
		keyboard.$.btnQty.$.button.tap,
		  function (wrapped) {
			customUpdateOrder_0();
			let result = wrapped.call(this, null);
		    return result;
		  }
		);
		
		keyboard.$.minusBtn.$.button.tap= _.wrap(
		keyboard.$.minusBtn.$.button.tap,
		  function (wrapped) {
			customUpdateOrder_0();
			let result = wrapped.call(this, null);
		    return result;
		  }
		);
		
		keyboard.$.plusBtn.$.button.tap= _.wrap(
		keyboard.$.plusBtn.$.button.tap,
		  function (wrapped) {
			customUpdateOrder_0();
			let result = wrapped.call(this, null);
		    return result;
		  }
		);
		
		keyboard.$.btnDiscount.$.button.tap= _.wrap(
		keyboard.$.btnDiscount.$.button.tap,
		  function (wrapped) {
			customUpdateOrder_0();
			let result = wrapped.call(this, null);
		    return result;
		  }
		);
		
		keyboard.$.toolbarcontainer.$.control.$.btnSide.$.code.$.button.tap= _.wrap(
		keyboard.$.toolbarcontainer.$.control.$.btnSide.$.code.$.button.tap,
		  function (wrapped) {
			customUpdateOrder_0();
			let result = wrapped.call(this, null);
		    return result;
		  }
		);
		
		rightPanel.$.toolbarpane.$.edit.$.editTabContent.$.actionButtonsContainer.$.deleteLine.tap= _.wrap(
		rightPanel.$.toolbarpane.$.edit.$.editTabContent.$.actionButtonsContainer.$.deleteLine.tap,
		  function (wrapped) {
			customUpdateOrder_0();
			let result = wrapped.call(this, null);
		    return result;
		  }
		);
		
	
		rightPanel.$.toolbarpane.$.payment.$.paymentTabContent.$.layawayaction.tap= _.wrap(
		rightPanel.$.toolbarpane.$.payment.$.paymentTabContent.$.layawayaction.tap,
		  function (wrapped) {
			customUpdateOrder_1();
			let result = wrapped.call(this, null);
		    return result;
		  }
		);
		
		
		
		OB.MobileApp.view.$.containerWindow.$.pointOfSale.$.multiColumn.$.rightToolbar.$.rightToolbar.$.toolbar.$.button3.$.theButton.$.toolbarBtnSearchCharacteristic.tap= _.wrap(
		OB.MobileApp.view.$.containerWindow.$.pointOfSale.$.multiColumn.$.rightToolbar.$.rightToolbar.$.toolbar.$.button3.$.theButton.$.toolbarBtnSearchCharacteristic.tap,
		  function (wrapped) {
			customUpdateOrder_0();
			let result = wrapped.call(this, null);
		    return result;
		  }
		);
		
		
	} 
}

function customUpdateOrder_0(){
	var order = OB.MobileApp.model.receipt;
	order.attributes.auxCustomValidation = true;
}

function customUpdateOrder_1(){
	var order = OB.MobileApp.model.receipt;
	order.attributes.auxCustomValidation = false;
}

	
	
	OB.OBPOSPointOfSale.UI.ButtonTabPayment.prototype.init = _.wrap(
	  OB.OBPOSPointOfSale.UI.ButtonTabPayment.prototype.init,
	  function (wrapped, model) {
	    var self = this; 
		funcCustomValidateOrderModified();
	    let result = wrapped.call(self, model);  
	    
	    return result;
	  }
	);

	
	
	OB.OBPOSPointOfSale.UI.ButtonTabPayment.prototype.tap= _.wrap(
	OB.OBPOSPointOfSale.UI.ButtonTabPayment.prototype.tap,
	  function (wrapped) {
		funcCustomLayawayPPlan();  
		funcCustomBtnFCation();
		var order = this.model.get('order');
	    var isLayaway = order.attributes.isLayaway;
		var isFullyDelivered = order.attributes.isFullyDelivered;
		var isPaid = order.attributes.isPaid;
		var isEditable = order.attributes.isEditable;
		var isModified = order.attributes.isModified;
		
		var gross = Number(order.get('gross'));
		if(gross < 0 ){
			var lines = order.get('lines');  
			var SearchImeis = false;
			var SearchOrder = "";
			
			_.forEach(lines.models, function (line) {
					var product = line.get('product');
					if(product && product != null){
						var isGarantizable = product.get('garantizable');
						if(isGarantizable && isGarantizable != null && isGarantizable == true){
							SearchImeis = true;
							SearchOrder = line.get('orderId');
						}
					}
			});
				
			if(SearchImeis && SearchImeis == true && SearchOrder && SearchOrder){
				funcSearchImeis(order, SearchOrder);
			}
		}
		
	    let result = wrapped.apply(this, null);
	    return result;
	  }
	);
	
	function funcSearchImeis(order, SearchOrder) { //Just for  credit Note
		var process = new OB.DS.Process('ec.com.sidesoft.retail.custompos.CustomSearchOrder');
		process.exec({
			data: {
		    id:SearchOrder,
		    isReturn:true
		  }
		}, function(data) {
			    if (data) {
					var lines = order.get('lines');
					var dataSearchImeis = data.imeis;
					var items = [];
					
					_.forEach(lines.models, function(line) {
						var stopIteration = false;
					    var product = line.get('product');
					    if (product && product != null) {
					        var isGarantizable = product.get('garantizable');
					        if (isGarantizable && isGarantizable != null && isGarantizable == true && !stopIteration) {
					            _.forEach(dataSearchImeis, function(objSearch) {
					                var currentProd = objSearch.prod;
					                if (currentProd == product.get('searchkey') && !items.includes(objSearch.imei) && !stopIteration) {
					                    line.set('Imei', objSearch.imei);
					                    line.set('telefono', objSearch.tlf);
					                    items.push(objSearch.imei);
					                    stopIteration = true; 
					                    //return false; // exit
					                }
					            });
					        }
					    }
					});
		    }
		    funcJustReadReturnInvoice(order);//Just for Credit Note
		}, function(error) {
		    console.error("Error al buscar pedido:", error);
		});
    }
    
    function funcJustReadReturnInvoice(order) {//Just for Credit Note
	if(order){
		const renderComboPaymentType = 
		OB.MobileApp.view.$.containerWindow.$.pointOfSale.$.multiColumn.$.leftPanel.$.receiptview.$.receiptheader.$.receiptLabels.$.paymentTypeSigma.$.renderComboPaymentType;
		
		const renderCanalType = 
		OB.MobileApp.view.$.containerWindow.$.pointOfSale.$.multiColumn.$.leftPanel.$.receiptview.$.receiptheader.$.receiptLabels.$.canalSigma.$.renderCanalType;  
		
		const custom = 
		OB.MobileApp.view.$.containerWindow.$.pointOfSale.$.multiColumn.$.leftPanel.$.receiptview.$.receiptheader.$.receiptButtons.$.bpbutton;
			
		const searchCustom = 
		OB.MobileApp.view.$.containerWindow.$.pointOfSale.$.multiColumn.$.leftPanel.$.receiptview.$.receiptheader.$.receiptButtons.$.OB_UI_BtnSIGCedula;
			
		const bplocbutton = 
		OB.MobileApp.view.$.containerWindow.$.pointOfSale.$.multiColumn.$.leftPanel.$.receiptview.$.receiptheader.$.receiptButtons.$.bplocbutton;
		
		  renderComboPaymentType.addStyles('pointer-events: none; background-color: #e2e2e2;');
          renderCanalType.addStyles('pointer-events: none; background-color: #e2e2e2;');
		  custom.setDisabled(true);
		  custom.addClass('btnkeyboard-inactive');
		  searchCustom.setDisabled(true);
		  searchCustom.addClass('btnkeyboard-inactive');
		  bplocbutton.setDisabled(true);
		  bplocbutton.addClass('btnkeyboard-inactive');

		
		
		var rightPanel = OB.MobileApp.view.$.containerWindow.$.pointOfSale.$.multiColumn.$.rightPanel;
		var actionsButtons = rightPanel.$.toolbarpane.$.edit.$.editTabContent.$.actionButtonsContainer;
		var btnDelete = actionsButtons.$.deleteLine;
		var btnPadlock = actionsButtons.$.validatePadlock;
		
		var btnDiscount = rightPanel.$.keyboard.$.btnDiscount.$.button;
		var btnUPC = rightPanel.$.keyboard.$.toolbarcontainer.$.control.$.btnSide.$.code.$.button;
		
		var toolbar = OB.MobileApp.view.$.containerWindow.$.pointOfSale.$.multiColumn.$.rightToolbar.$.rightToolbar.$.toolbar;
		
		var btnEditLine = toolbar.$.button4.$.theButton.$.toolbarBtnEdit;
		var btnSearch = toolbar.$.button3.$.theButton.$.toolbarBtnSearchCharacteristic;
		var btnNavi = toolbar.$.button2.$.theButton.$.toolbarBtnCatalog;
		var btnScan = toolbar.$.button.$.theButton.$.toolbarBtnScan;
		
			btnDelete.setDisabled(true);
			btnDelete.addClass('btnkeyboard-inactive');
			btnPadlock.setDisabled(true);
			btnPadlock.addClass('btnkeyboard-inactive');
			
			btnDiscount.setDisabled(true);
			btnDiscount.addClass('btnkeyboard-inactive');
			
			btnEditLine.setDisabled(true);
			btnEditLine.addClass('btnkeyboard-inactive');
			btnSearch.setDisabled(true);
			btnSearch.addClass('btnkeyboard-inactive');
			btnNavi.setDisabled(true);
			btnNavi.addClass('btnkeyboard-inactive');
			btnScan.setDisabled(true);
			btnScan.addClass('btnkeyboard-inactive');
			btnUPC.setDisabled(true);
			btnUPC.addClass('btnkeyboard-inactive');
			
		
	}
  }
	
	function funcCustomBtnFCation(){
		OB.MobileApp.view.$.containerWindow.$.pointOfSale.$.multiColumn.$.rightPanel.$.toolbarpane.$.payment.$.paymentTabContent.$.exactbutton.tap= _.wrap(
		OB.MobileApp.view.$.containerWindow.$.pointOfSale.$.multiColumn.$.rightPanel.$.toolbarpane.$.payment.$.paymentTabContent.$.exactbutton.tap,
		  function (wrapped) {
			OB.MobileApp.view.$.containerWindow.$.pointOfSale.$.multiColumn.$.rightPanel.$.toolbarpane.$.payment.$.paymentTabContent.$.creditsalesaction.removeClass('btnkeyboard-inactive');
	        OB.MobileApp.view.$.containerWindow.$.pointOfSale.$.multiColumn.$.rightPanel.$.toolbarpane.$.payment.$.paymentTabContent.$.creditsalesaction.applyStyle("pointer-events", null);
			OB.MobileApp.view.$.containerWindow.$.pointOfSale.$.multiColumn.$.rightPanel.$.toolbarpane.$.payment.$.paymentTabContent.$.creditsalesaction.setLocalDisabled(false);

			var creditsalesaction = OB.MobileApp.view.$.containerWindow.$.pointOfSale.$.multiColumn.$.rightPanel.$.toolbarpane.$.payment.$.paymentTabContent.$.creditsalesaction;
			//console.log("ACTIVANDO",creditsalesaction);
			let result = wrapped.call(this, null);
		    return result;
		  }
		);
	}
	
	function funcCustomLayawayPPlan(){
		var menuLeft = OB.MobileApp.view.$.containerWindow.$.pointOfSale.$.multiColumn.$.leftToolbar.$.leftToolbar.$.leftHolder.$.mainMenu.$.menuHolder.$.mainMenuButton;
			
		var combo = OB.MobileApp.view.$.containerWindow.$.pointOfSale.$.multiColumn.$.leftPanel.$.receiptview.$.receiptheader.$.receiptLabels.$.paymentTypeSigma.$.renderComboPaymentType.getValue();
		     
		     var order = OB.MobileApp.model.receipt.attributes;
		     
		     var isLayaway = order.isLayaway
		     var isFullyDelivered=order.isFullyDelivered
		     var isEditable=order.isEditable
		     var isModified=order.isModified
		     
		     
		     var CustomLayawayPPlan = order.funcCustomLayawayPPlan;
			if(combo === 'SIG_PlanPagos' && CustomLayawayPPlan != true){
				if(!isLayaway  && !isFullyDelivered && isEditable && !isModified ){
					//console.log("%cINGRESA","color:green",CustomLayawayPPlan);
					menuLeft.tap();
					var layaWay = OB.MobileApp.view.$.containerWindow.$.pointOfSale.$.multiColumn.$.leftToolbar.$.leftToolbar.$.leftHolder.$.mainMenu.$.menuHolder.$.menuScroller.$.menuLayaway;
					layaWay.tap();
					OB.MobileApp.model.receipt.attributes.funcCustomLayawayPPlan = true;
				}
			}
			
	}
	
	OB.UTIL.HookManager.registerHook('OBPOS_OrderDetailContentHook',function (args, callbacks) {
	var layaWay = OB.MobileApp.view.$.containerWindow.$.pointOfSale.$.multiColumn.$.leftToolbar.$.leftToolbar.$.leftHolder.$.mainMenu.$.menuHolder.$.menuScroller.$.menuLayaway;
	layaWay.hide();	
	OB.UTIL.HookManager.callbackExecutor(args, callbacks);
	});
	
	OB.OBPOSPointOfSale.UI.ButtonTabPayment.prototype.renderTotal= _.wrap(
	OB.OBPOSPointOfSale.UI.ButtonTabPayment.prototype.renderTotal,
	  function (wrapped, inSender, inEvent) {
		var order = OB.MobileApp.model.receipt;
		
		funcCustomJustReadKey(order)
		
	    let result = wrapped.call(this, inSender, inEvent);
	    return result;
	  }
	);
	
	function funcCustomJustReadKey(order){
		var combo = OB.MobileApp.view.$.containerWindow.$.pointOfSale.$.multiColumn.$.leftPanel.$.receiptview.$.receiptheader.$.receiptLabels.$.paymentTypeSigma.$.renderComboPaymentType.getValue();
		var isLayaway = order.attributes.isLayaway;
		var isPaid = order.attributes.isPaid;
		var isEditable = order.attributes.isEditable;
		var isModified = order.attributes.isModified;
		var isNewReceipt = order.attributes.isNewReceipt;
		
		var toolbarPayment =
			 OB.MobileApp.view.$.containerWindow.$.pointOfSale.$.multiColumn.$.rightPanel.$.keyboard.$.toolbarcontainer.$.toolbarPayment;
		var control =
			 OB.MobileApp.view.$.containerWindow.$.pointOfSale.$.multiColumn.$.rightPanel.$.keyboard.$.control;
		
		if(combo === 'SIG_PlanPagos'){//Prefecture
		var currentGros = Number(order.get('gross'));
		  if((!isEditable && isLayaway && !isModified && !isNewReceipt && !isPaid) || (currentGros < 0)){
			 var existingStyles = toolbarPayment.domStyles;
			  if (existingStyles && existingStyles['pointer-events'] == ' none') {
			    toolbarPayment.applyStyle('pointer-events', null);
			    //control.applyStyle('pointer-events', null);
			  }
			 
		  }else{
			
			toolbarPayment.addStyles('pointer-events: none');
		    //control.addStyles('pointer-events: none');
			 
		  }
		}else{
			var existingStyles = toolbarPayment.domStyles;
			  if (existingStyles && existingStyles['pointer-events'] == ' none') {
			    toolbarPayment.applyStyle('pointer-events', null);
			    //control.applyStyle('pointer-events', null);
			  }
		}
		
	}
	
	

	
})();	
