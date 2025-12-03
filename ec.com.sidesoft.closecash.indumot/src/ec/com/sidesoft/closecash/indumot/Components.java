package ec.com.sidesoft.closecash.indumot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class Components {

    //Metodo
	public int metodocustom(int a, int b) {
        a = a + 1;
        return a * b;
    }

    //Metodo
	public int metodocustom2(int a, int b) {
        a = a + 1;
        return a / b;
    }

    public int metodocustom3(int a, int c){
        int sum = a+ c;
        return sum;
    }

    public void SetAdminNow(){
        try{
             OBContext.setAdminMode();


        }catch(Exception e){

        } finally {
            OBContext.restorePreviousMode(); 
        }
    }
}
