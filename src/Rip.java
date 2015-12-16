import java.io.FileInputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Scanner;
import java.util.TreeMap;

import RIP.Servidor;
import RIP.Vecino;
import RIP.Subred;

/**PROYECTO DE REDES DE ORDENADORES - CURSO 2014/2015 - UNIVERSIDAD DE VIGO.
 * Implementación de un protocolo de encaminamiento mediante vector de distancias.
 * 
 * 
 * Las mejoras introducidas por nosotros para este proyecto son:
 * 		-> Compatibilidad con UDP.
 * 		-> Autentificación mediante contraseña.
 * 		-> Split horizon.
 * 
 * Este proyecto calcula las distancias en menor número de saltos hacia la red o router objetivo. 
 * 
 * Las topologías probadas tanto en laboratorio como por nosotros dieron resultados favorables, si bien creemos que no todas las posibilidades han sido exploradas con la suficiente profundidad y que podrían darse casos no contemplados en el código en los cuales el código no funcione correctamente.
 * 
 * La interfaz por defecto será considerada la 'eth0'; y las máscaras de las diferentes subredes se tratan en decimal.
 * 
 * En relación a Split horizon, debemos decir que hemos hecho una interpretación un poco 'subjetiva' de su funcionamiento; lo que queremos decir con esto es que estuvimos probando diferentes métodos de implementar esta mejora y no conseguimos que ninguno funcionase correctamente, aunque el programa se comporta, mediante otras comprobaciones (seguramente no tan directas), como se describe para Split Horizon.
 * 
 * @author Marcos Pires Filgueira
 * @author Pedro Tubío Figueira
 *
 */


public class Rip {
	
	/**Método principal del protocolo. Es el que inicializa el sistema.
	 * 
	 * 
	 * @param args Contraseña.
	 */
	public static void main(String[] args){
		
		//Declaramos las variables que vamos a utilizar en el método 'main'.
		TreeMap<Integer, Vecino> listadoVecinos = new TreeMap<Integer, Vecino>();
		TreeMap<Integer, Subred> listadoSubredes= new TreeMap<Integer, Subred>();
		String password = null;
		
		if (args.length != 0) password=setPassword(args[0]);
		
		//Obtenemos la IP de la interfaz 'eth0'.
		String IP = getIPInterfaceEth0();
		System.out.println("IP del HOST: " + "'" + IP + "'" + "\tInterfaz de salida: 'eth0'.");
		
		//Leemos el archivo de configuración
		leerArchivoConfiguracion(listadoSubredes, listadoVecinos, password);
		
		//Imprimimos la tabla con nuestros datos
		Servidor.imprimirTablaVecinos(listadoVecinos, IP);
		
		//Ejecutamos el servidor
		Servidor servidor = new Servidor();
		servidor.ejecucion(listadoVecinos, listadoSubredes, password, getIPInterfaceEth0());
		
		
	}	
	
	/** Método que coge la contraseña de argumento.
	 * 
	 * @param password Contraseña de autentificación
	 * @return
	 */
	public static String setPassword(String password){
		
		String pass = null;
		
		try{
			pass = password.trim();
		}catch (ArrayIndexOutOfBoundsException e){
			System.out.println("No se ha introducido ninguna contraseña de autentificación.");
		}
		
		return pass;
	}
	
	/**Método exclusivo para obtener la IP de la interfaz 'eth0' del host en el que ejecutemos el código.
	 * 
	 * Está adaptado para funcionar en los hosts 'Ubuntu' del laboratorio. Puede darse el caso de que funcione en Windows, pero no es lo habitual.
	 * 
	 * Más información: http://stackoverflow.com/questions/494465/how-to-enumerate-ip-addresses-of-all-enabled-nic-cards-from-java
	 * 
	 * @return IP de la interfaz 'eth0'.
	 */
	public static String getIPInterfaceEth0(){

			String IP = null;
				
			try {
				Enumeration<InetAddress>  listadoInetAddresses = NetworkInterface.getByName("eth0").getInetAddresses();
				
				while (listadoInetAddresses.hasMoreElements()) {
					InetAddress inetAddressTemp = listadoInetAddresses.nextElement();
						
					if (inetAddressTemp instanceof Inet4Address) {
						IP = inetAddressTemp.getHostAddress();
					}
				}
			} catch (SocketException e) {
				System.out.println("Error a la hora de coger la IP perteneciente a la interfaz 'eth0'. Vuelva a ejecutar de nuevo el programa asegurándose de que su host está conectado a una red mediante el puerto Ethernet.");
				System.exit(0);
			}
			
		return IP;
	}

	/** Leemos del archivo de configuración ripconf-'IP'.txt la red con la que estamos trabajando; es decir, formamos la topología con los vecinos que conozcamos.

	 * 
	 * @param listadoSubredes Listado de las subredes que conoce el host local.
	 * @param listadoVecinos Listado de los routers vecinos al host local.
	 * @param password Contraseña de autentificación a la red. Deberá ser menor o igual a 16 bits.
	 */
	public static void leerArchivoConfiguracion(TreeMap<Integer, Subred> listadoSubredes, TreeMap<Integer,Vecino> listadoVecinos, String password){
		
		String ficheroALeer = "ripconf-" + getIPInterfaceEth0() +".txt"; //Estructura del nombre del archivo de configuración.
		
		try{
			Scanner punteroLectura = new Scanner (new FileInputStream(ficheroALeer));
			
			int i=0, j=0;
			
			while(punteroLectura.hasNext()){
				
	 			String IPArchivo;
	 			int distancia=1;
	 			String mascara;
	 			
	 			String linea = punteroLectura.nextLine();
	 			
	 			
	 			String[] lineasSeparadas = linea.split("/");
	 			
	 			//Si se cumple la condición, es una red que conocemos.
	 			if(lineasSeparadas.length==2){ 				//lineasSeparadas[0] -> IP | lineasSeparadas[1] -> Máscara subred
	 				IPArchivo = lineasSeparadas[0];
	 				mascara = lineasSeparadas[1];
	 				
	 				Subred subred = new Subred(IPArchivo, distancia, mascara, password, IPArchivo);
	 				listadoSubredes.put(j,subred);
	 				
	 				j++;
	 				
	 			//Si no se cumple la condición, es un vecino, y lo incluímos en la lista de routers vecinos.
	 			} else{
	 				IPArchivo = lineasSeparadas[0];
	 				
	 				Vecino routerVecino = new Vecino(IPArchivo, distancia, getIPInterfaceEth0(), password);
	 				listadoVecinos.put(i,routerVecino);
		 			
	 				i++;
	 			}
			}
			
			punteroLectura.close();
			
		} catch (IOException e){
			System.out.println("Error a la hora de leer el archivo de configuración de la red. Por favor, asegúrese de que el archivo tiene la IP del host y que se encuentra en el directorio raíz del programa y vuelva a intentarlo.");
			System.exit(0);
		} 
		
		
	}	

}