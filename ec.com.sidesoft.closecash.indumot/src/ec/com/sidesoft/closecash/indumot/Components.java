package ec.com.sidesoft.closecash.indumot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class Components {

    //metodo test pruebas
    public void SetAdminNow(){ 
        try{
             OBContext.setAdminMode();

        }catch(Exception e){

        } finally {
            OBContext.restorePreviousMode(); 
        }
    }
}
