package ec.com.sidesoft.closecash.indumot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class Components { 
  
    /**
     * Uso controlado de modo admin para operación técnica interna.
     * Contexto restaurado inmediatamente.
     */
    public void executeAsAdminForInternalProcess() {
        OBContext.setAdminMode();
        try {
            org.openbravo.model.pricing.priceadjustment.Product offerProduct = OBProvider.getInstance().get(org.openbravo.model.pricing.priceadjustment.Product.class);
            offerProduct.setActive(true);
            offerProduct.setClient(offer.getClient());
            offerProduct.setOrganization(offer.getOrganization());
            offerProduct.setProduct(productObj);
            offerProduct.setPriceAdjustment(offer);
            offerProduct.setSHPDIPrice(Parameter.BIGDECIMAL.parse(price));
            OBDal.getInstance().save(offerProduct);
        } finally {
            OBContext.restorePreviousMode();
        }
    }

    public int multiplicarAux(int a, int b) { 
        return a * b;
    }


}
