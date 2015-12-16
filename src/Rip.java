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
 * Implementaci�n de un protocolo de encaminamiento mediante vector de distancias.
 * 
 * 
 * Las mejoras introducidas por nosotros para este proyecto son:
 * 		-> Compatibilidad con UDP.
 * 		-> Autentificaci�n mediante contrase�a.
 * 		-> Split horizon.
 * 
 * Este proyecto calcula las distancias en menor n�mero de saltos hacia la red o router objetivo. 
 * 
 * Las topolog�as probadas tanto en laboratorio como por nosotros dieron resultados favorables, si bien creemos que no todas las posibilidades han sido exploradas con la suficiente profundidad y que podr�an darse casos no contemplados en el c�digo en los cuales el c�digo no funcione correctamente.
 * 
 * La interfaz por defecto ser� considerada la 'eth0'; y las m�scaras de las diferentes subredes se tratan en decimal.
 * 
 * En relaci�n a Split horizon, debemos decir que hemos hecho una interpretaci�n un poco 'subjetiva' de su funcionamiento; lo que queremos decir con esto es que estuvimos probando diferentes m�todos de implementar esta mejora y no conseguimos que ninguno funcionase correctamente, aunque el programa se comporta, mediante otras comprobaciones (seguramente no tan directas), como se describe para Split Horizon.
 * 
 * @author Marcos Pires Filgueira
 * @author Pedro Tub�o Figueira
 *
 */


public class Rip {
	
	/**M�todo principal del protocolo. Es el que inicializa el sistema.
	 * 
	 * 
	 * @param args Contrase�a.
	 */
	public static void main(String[] args){
		
		//Declaramos las variables que vamos a utilizar en el m�todo 'main'.
		TreeMap<Integer, Vecino> listadoVecinos = new TreeMap<Integer, Vecino>();
		TreeMap<Integer, Subred> listadoSubredes= new TreeMap<Integer, Subred>();
		String password = null;
		
		if (args.length != 0) password=setPassword(args[0]);
		
		//Obtenemos la IP de la interfaz 'eth0'.
		String IP = getIPInterfaceEth0();
		System.out.println("IP del HOST: " + "'" + IP + "'" + "\tInterfaz de salida: 'eth0'.");
		
		//Leemos el archivo de configuraci�n
		leerArchivoConfiguracion(listadoSubredes, listadoVecinos, password);
		
		//Imprimimos la tabla con nuestros datos
		Servidor.imprimirTablaVecinos(listadoVecinos, IP);
		
		//Ejecutamos el servidor
		Servidor servidor = new Servidor();
		servidor.ejecucion(listadoVecinos, listadoSubredes, password, getIPInterfaceEth0());
		
		
	}	
	
	/** M�todo que coge la contrase�a de argumento.
	 * 
	 * @param password Contrase�a de autentificaci�n
	 * @return
	 */
	public static String setPassword(String password){
		
		String pass = null;
		
		try{
			pass = password.trim();
		}catch (ArrayIndexOutOfBoundsException e){
			System.out.println("No se ha introducido ninguna contrase�a de autentificaci�n.");
		}
		
		return pass;
	}
	
	/**M�todo exclusivo para obtener la IP de la interfaz 'eth0' del host en el que ejecutemos el c�digo.
	 * 
	 * Est� adaptado para funcionar en los hosts 'Ubuntu' del laboratorio. Puede darse el caso de que funcione en Windows, pero no es lo habitual.
	 * 
	 * M�s informaci�n: http://stackoverflow.com/questions/494465/how-to-enumerate-ip-addresses-of-all-enabled-nic-cards-from-java
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
				System.out.println("Error a la hora de coger la IP perteneciente a la interfaz 'eth0'. Vuelva a ejecutar de nuevo el programa asegur�ndose de que su host est� conectado a una red mediante el puerto Ethernet.");
				System.exit(0);
			}
			
		return IP;
	}

	/** Leemos del archivo de configuraci�n ripconf-'IP'.txt la red con la que estamos trabajando; es decir, formamos la topolog�a con los vecinos que conozcamos.

	 * 
	 * @param listadoSubredes Listado de las subredes que conoce el host local.
	 * @param listadoVecinos Listado de los routers vecinos al host local.
	 * @param password Contrase�a de autentificaci�n a la red. Deber� ser menor o igual a 16 bits.
	 */
	public static void leerArchivoConfiguracion(TreeMap<Integer, Subred> listadoSubredes, TreeMap<Integer,Vecino> listadoVecinos, String password){
		
		String ficheroALeer = "ripconf-" + getIPInterfaceEth0() +".txt"; //Estructura del nombre del archivo de configuraci�n.
		
		try{
			Scanner punteroLectura = new Scanner (new FileInputStream(ficheroALeer));
			
			int i=0, j=0;
			
			while(punteroLectura.hasNext()){
				
	 			String IPArchivo;
	 			int distancia=1;
	 			String mascara;
	 			
	 			String linea = punteroLectura.nextLine();
	 			
	 			
	 			String[] lineasSeparadas = linea.split("/");
	 			
	 			//Si se cumple la condici�n, es una red que conocemos.
	 			if(lineasSeparadas.length==2){ 				//lineasSeparadas[0] -> IP | lineasSeparadas[1] -> M�scara subred
	 				IPArchivo = lineasSeparadas[0];
	 				mascara = lineasSeparadas[1];
	 				
	 				Subred subred = new Subred(IPArchivo, distancia, mascara, password, IPArchivo);
	 				listadoSubredes.put(j,subred);
	 				
	 				j++;
	 				
	 			//Si no se cumple la condici�n, es un vecino, y lo inclu�mos en la lista de routers vecinos.
	 			} else{
	 				IPArchivo = lineasSeparadas[0];
	 				
	 				Vecino routerVecino = new Vecino(IPArchivo, distancia, getIPInterfaceEth0(), password);
	 				listadoVecinos.put(i,routerVecino);
		 			
	 				i++;
	 			}
			}
			
			punteroLectura.close();
			
		} catch (IOException e){
			System.out.println("Error a la hora de leer el archivo de configuraci�n de la red. Por favor, aseg�rese de que el archivo tiene la IP del host y que se encuentra en el directorio ra�z del programa y vuelva a intentarlo.");
			System.exit(0);
		} 
		
		
	}	

}