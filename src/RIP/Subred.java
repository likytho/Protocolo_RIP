package RIP;

/**
 * Este objeto permite crear clases del tipo "Subred", que hacen de homólogo a las subredes del proyecto.
 * 
 * @author Marcos Pires Filgueira
 * @author Pedro Tubío Figueira
 *
 */

public class Subred {

	public String IP;
	public int distancia;
	public String mascara;
	public String password;
	public String siguienteSalto = null;
	
	/**Constuctor general de la clase.
	 * 
	 * 
	 * @param IP IP de la subred.
	 * @param distancia Distancia de la subred.
	 * @param mascara Máscara de la subred.
	 * @param password Contraseña de autentificación.
	 * @param siguienteSalto Siguiente salto hacia la subred.
	 */
	public Subred(String IP, int distancia, String mascara, String password, String siguienteSalto){
		this.IP=IP;
		this.distancia=distancia;
		this.mascara=mascara;
		this.password=password;
		this.siguienteSalto=siguienteSalto;
	}

	//Típicos getters y setters
	public String getIP() {
		return IP;
	}

	public int getDistancia() {
		return distancia;
	}

	public String getMascara() {
		return mascara;
	}
	
	public String getPassword(){
		return this.password;
	}
	
	public String getSiguienteSalto(){
		return this.siguienteSalto;
	}
	
	public void setSiguienteSalto(String siguienteSalto){
		this.siguienteSalto=siguienteSalto;
	}

}
