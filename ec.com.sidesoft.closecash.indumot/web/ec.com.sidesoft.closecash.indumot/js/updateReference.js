OB.OBPFCI = {};
OB.OBPFCI.ClientSideEventHandlers = {};
OB.OBPFCI.SSCCCIN_FIN_TRANSACTION_TAB = '23691259D1BD4496BCC5F32645BCA4B9';
console.log(OB.OBPFCI.SSCCCIN_FIN_TRANSACTION_TAB);
OB.OBPFCI.ClientSideEventHandlers.updateReference = function (view, form, grid, extraParameters, actions) {
  var data = extraParameters.data, id, callback;

  id = data.id;

  console.log(id);

  callback = function (response, cdata, request) {
    console.log(cdata);
    OB.EventHandlerRegistry.callbackExecutor(view, form, grid, extraParameters, actions);
  };

  // Calling action handler
  OB.RemoteCallManager.call('ec.com.sidesoft.closecash.indumot.ad_actions.UpdateReferenceActionHandler', {
    id: id
  }, {}, callback);
};

OB.EventHandlerRegistry.register(OB.OBPFCI.SSCCCIN_FIN_TRANSACTION_TAB, OB.EventHandlerRegistry.POSTSAVE, OB.OBPFCI.ClientSideEventHandlers.updateReference, 'SSCCCIN_UpdateReference');