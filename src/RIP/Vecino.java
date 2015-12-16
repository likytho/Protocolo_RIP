package RIP;

/**Clase que permite emular como objetos a los routers vecinos del nuestro.
 * 
 * 
 * @author Marcos Pires Filgueira
 * @author Pedro Tubío Figueira
 *
 */
public class Vecino {

	private int distancia;
	private String IP;
	private String siguienteSalto;
	private String password;
	
	/**Constructor genérico del objeto.
	 * 
	 * 
	 * @param IP IP del router.
	 * @param distancia Distancia con el router.
	 * @param siguienteSalto Siguiente salto hacia el router.
	 * @param password Contraseña de autentificación.
	 */
	public Vecino(String IP, int distancia, String siguienteSalto, String password){ 
		this.distancia = distancia;
		this.IP=IP;
		this.siguienteSalto=siguienteSalto;		
		this.password=password;
	}

	public int getDistancia(){
		return this.distancia;
	}
	
	public String getIP(){
		return this.IP;
	}
	
	public String getSiguienteSalto(){
		return this.siguienteSalto;
	}
	
	public String getPassword(){
		return this.password;
	}
	
}
