package RIP;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.Map.Entry;
/**Clase servidor. En esta clase, de forma general, tratamos el apartado de recepción de datos por parte de todos los nodos que conforman la red y los gestinoamos de tal forma que nos permita encontrar la ruta más óptima entre 2 extremos cualesquiera de la red.
 * 
 * Por supuesto, cada una de las partes contiene la explicación correspondiente a su funcionalidad dentro del sistema.
 * 
 * @author Marcos Pires Filgueira
 * @author Pedro Tubío Figueira
 *
 */

public class Servidor {
	
	//Puerto UDP en el que se llevarán a cabo las conexiones. RIP, por defecto, utiliza el puerto 520, pero el sistema no nos lo permite si no somos administradores. 
	public final static int PUERTO_UDP = 25000;
	
	/**
	 * Ejecución de la parte troncal del código. Aquí se llevan a cabo las operaciones sobre el cálculo de la topología de la red.
	 * 
	 * 
	 * @param listadoVecinos Lista ordenada de todos los vecinos que conoce el host.
	 * @param listadoSubredes Lista ordenada de todas las subredes que conoce el host.
	 * @param password Contraseña de autentificación.
	 * @param IPLocal IP del host local.
	 */
	@SuppressWarnings("rawtypes")
	public void ejecucion(TreeMap<Integer, Vecino> listadoVecinos, TreeMap<Integer, Subred> listadoSubredes, String password, String IPLocal){
		
		//Declaración de variables
		DatagramSocket UDP = null;
		DatagramPacket paquete = null;
		
		String mensajeAEnviar = "";
		byte[] mensajeBinario = null;
		
		TreeMap<Integer, Vecino> listadoRutasConectadas = new TreeMap<Integer, Vecino>();
		
		//Primera iteracción con la red: envio la información que poseo (me anuncio a la red).
		
		//Creamos el socket
		try {
			UDP = new DatagramSocket(PUERTO_UDP);
		} catch (SocketException e1) {
			System.out.println("Error a la hora de crear el Socket.");
		}
		
		//Creamos una lista con las rutas que están directamente conectadas a nuestro host (de los datos obtenidos de la lectura del archivo de configuración.
		Iterator iteradorVecinos = listadoVecinos.keySet().iterator();
		
		if(!listadoVecinos.isEmpty()){
			while(iteradorVecinos.hasNext()){
				Integer key = (Integer) iteradorVecinos.next();
				Vecino vecinoTemp = listadoVecinos.get(key);
				
				listadoRutasConectadas.put(key, vecinoTemp);
			
			}
		}
		
		//Mediante un bucle infinito, creamos el mensaje con los datos necesarios para generar la tabla de reenvío con la ayuda de nuestros vecinos.
		while(true){
			try{
				//Generamos el mensaje mediante un método.
				mensajeAEnviar = crearListaVecinos(listadoVecinos, listadoSubredes, IPLocal, password);
				//Traducimos a binario el mensaje
				mensajeBinario = mensajeAEnviar.getBytes("UTF-8");
				
				//A cada vecino con el que tengamos una ruta conectada, le enviamos el paquete con los datos.
				Iterator iteradorRutasConectadas = listadoRutasConectadas.keySet().iterator();
				
				if(!listadoRutasConectadas.isEmpty()){
					while(iteradorRutasConectadas.hasNext()){
						
						Integer key = (Integer) iteradorRutasConectadas.next();
						Vecino vecinoTemp = listadoVecinos.get(key);
						
						paquete = new DatagramPacket (mensajeBinario, mensajeBinario.length, InetAddress.getByName(vecinoTemp.getIP()), 25000);
						UDP.send(paquete);
						
					}
				}				
			} catch (SocketException e){
				System.out.println("Error: no se ha podido crear el socket correctamente. Vuelva a intentarlo.");
				System.exit(0);
			} catch (UnsupportedEncodingException e){
				
			} catch (UnknownHostException e){
				System.out.println("Error a la hora de enviar el paquete al hosts de destino.");
			} catch (IOException e){
				System.out.println("Error de conexión.");
			}
			
			//Estamos a la escucha de recibir algún paquete de nuestros vecinos.
			recibirPaquetes(UDP, listadoVecinos, listadoSubredes,password, IPLocal);
			
			//Imprimimos la información sobre nuestros vecinos y las subredes que hayamos descubierto.
			imprimirTablaVecinos(listadoVecinos, IPLocal);
			imprimirTablaSubredes(listadoSubredes, IPLocal);
			
			//Actualizamos la lista de rutas conectadas, en caso de que se haya agregado a la red algún nuevo router.
			Iterator iteradorVecinosActualizador = listadoVecinos.keySet().iterator();
			
			if(!listadoVecinos.isEmpty()){
				while(iteradorVecinosActualizador.hasNext()){
					Integer key = (Integer) iteradorVecinosActualizador.next();
					Vecino vecinoTemp = listadoVecinos.get(key);
					
					if(vecinoTemp.getDistancia()==1){
						listadoRutasConectadas.put(key, vecinoTemp);
					}
				}
			}	
		}
	}

	/** Método que se encarga de recibir el paquete y trabaja con los datos de tal forma que puedan ser comparados facilmente con los preexistentes para generar la topología de red mediante el vector de distancias.
	 * 
	 * 
	 * @param UDP Socket de conexión.
	 * @param listadoVecinos Listado con los vecinos del host.
	 * @param listadoSubredes Listado con las subredes que conoce el host.
	 * @param password Contraseña de autentificación de la red.
	 * @param IPLocal IP del host.
	 */
	public void recibirPaquetes(DatagramSocket UDP, TreeMap<Integer, Vecino> listadoVecinos, TreeMap<Integer, Subred> listadoSubredes, String password, String IPLocal){
		
		//Declaración de variables
		byte[] buf = new byte[1000];
	    DatagramPacket dp = new DatagramPacket(buf, buf.length);
	    long TimeOut=10000;
	    
	    //Asignación del tiempo de espera, para mantenernos a la escucha durante 10 segundos.
	    try {
			UDP.setSoTimeout((int)TimeOut);
		} catch (SocketException e) {
				// TODO Auto-generated catch bloc
			System.out.println("TIEMPO DE ESPERA AGOTADO");
		}
	    
	    //capturamos la hora de inicio 
	    Date horaInicio = new Date();
	    boolean continuar = true;
	    String IPRecibidaUDP="";
	    
		while(continuar){
			try {
				UDP.receive(dp);
				
				//Recibimos los datagramas y separamos los "bloques" con el elemento "#"
				String rcvd =new String(dp.getData(), 0, dp.getLength());
		    	String[] mensajeSeparado= rcvd.split("#");
		    	
		    	//Al tiempo de espera, le descontamos el tiempo consumido hasta el momento y permanecemos a la escucha nuevamente.
		    	Date hora = new Date();
		        TimeOut= 10000- (hora.getTime()-horaInicio.getTime());
		        UDP.setSoTimeout((int)TimeOut);
		        
		        boolean todoOK=false;

		        //Filtramos cada bloque separandolo por "-" para conseguir los diferentes parametros de las subredes(comienzan por un "0") y de los routers vercinos (comienzan por un "1")
		        for(int i=0;i<mensajeSeparado.length; i++){
		        
		        	String[] elementoSeparado = mensajeSeparado[i].split("-");
		        	
		        	if(todoOK){
		        		if(elementoSeparado[0].equals("0")){//subRed
		        			String IP = elementoSeparado[1];
		        			String Mascara = elementoSeparado[2];
		        			String distancia = elementoSeparado[3];
		
		        			compararTablasRedes(listadoSubredes, IP,Mascara, password, distancia, IPLocal, IPRecibidaUDP);
		        		
		        		}else if(elementoSeparado[0].equals("1")){ //Vecino
		        			String IP = elementoSeparado[1];
		        			String sigSalto = elementoSeparado[2];
		        			String distancia = elementoSeparado[3];

		        			compararTablasVecinos(listadoVecinos, IP, distancia,sigSalto, password, IPLocal);	
		        		}	
	        	}		        	
		        
		       	//En la primera iteración filtramos los datos recibidos, recogiendo el router vecino en una ruta directamente conectada y comprobamos que la contraseña sea la correcta.
		        if(i==0){
		        	
		        		String passwordRecibida = elementoSeparado[0];
		        		IPRecibidaUDP = elementoSeparado[1];

		        		if(password != null){
		        			if(password.equals(passwordRecibida)){
			        			todoOK=true;
			        		}	
		        		} else {
		        			if(passwordRecibida == null) todoOK = true;
		        		}
		        		
		        		if(todoOK){
		        			String IP= elementoSeparado[1];
		        			String sigSalto=IP;
		        			compararTablasVecinos(listadoVecinos, IP, "0", sigSalto, password, IPLocal);
		        		}
		        	}
		    }
			}catch(SocketTimeoutException e){
				continuar=false;
				
			}catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**Método que compara las tablas de reenvío de las subredes para buscar coincidencias y redundancias.
	 * 
	 * 
	 * 
	 * @param listadoSubredes Listado con las subredes que conoce el host.
	 * @param IP IP contenida en la trama UDP que hemos recibido de algún vecino.
	 * @param Mascara Máscara de alguna subred.
	 * @param password Contrasñea de autentificación de la red.
	 * @param distancia Distancia a la que se encuentra la red.
	 * @param IPLocal IP del host.
	 * @param IPRecibidaUDP IP del origen de la trama UDP.
	 * 
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void compararTablasRedes (TreeMap<Integer, Subred> listadoSubredes,String IP,String Mascara,String password,String distancia, String IPLocal, String IPRecibidaUDP){
		//Declaracion del iterador con el que recorreremos la lista de las subRedes y declaración de un nuevo mapa que usaremos para guardar los elementos a añadir, de forma temporal.
		Iterator itSubredes = listadoSubredes.keySet().iterator();
		TreeMap<Integer, Subred> listaTemporal = new TreeMap();
	
		//Recorremos toda la lista en busca de alguna coincidencia con la IP que hemos recibido.
		boolean nuevaSubredBol = true;
		while(itSubredes.hasNext()){
			Integer key =(Integer) itSubredes.next();
			Subred subRedTemp = listadoSubredes.get(key);
		
			String IPTemp = subRedTemp.getIP();
			int distanciaTemp = subRedTemp.getDistancia();
			String mascaraTemp = subRedTemp.getMascara();
			
			
			//Si encontramos alguna IP igual comprobamos que la longitud de la máscara sea la misma, si esto es asi estamos en la misma subRed y actualizamos la distancia y ruta de ser necesario. Si las maáscaras no coinciden simplemente se añade.
			if(IPTemp.equals(IP)){
				
				if(mascaraTemp.equals(Mascara)){
					nuevaSubredBol=false;
		
					if(Integer.parseInt(distancia)+1<distanciaTemp){	
						int nuevaDistancia=Integer.parseInt(distancia)+1;
						Subred subredActualizada = new Subred(IPTemp, nuevaDistancia,mascaraTemp, password, IPRecibidaUDP);
						listaTemporal.put(key, subredActualizada);
					}
				}
			} //Si la IP recibida, coincide con nuestra propia IP local descartamos las tramas.
				if(IP.equals(IPLocal)){
					nuevaSubredBol=false;
				}
		}
		// Si hasta aqui no hemos realizado ningún cambio ni nada, significa que la subRed no esta en la lista y habría que añadirla.
		if(nuevaSubredBol){
			int nuevaKey =listadoSubredes.size()+1;
			int nuevaDistancia= Integer.parseInt(distancia)+1;
			
			Subred nuevaSubred = new Subred(IP, nuevaDistancia , Mascara, password, IPRecibidaUDP);
			listaTemporal.put(nuevaKey, nuevaSubred);
		}
		
		Iterator itTemporal = listaTemporal.keySet().iterator();
		
		//Actualizamos la lista con la lista temporal que hemos modificado hasta ahora.
		while(itTemporal.hasNext()){
			
			Integer keys =(Integer) itTemporal.next();
			Subred subRedTemporal = listaTemporal.get(keys);
			
			String IPNueva = subRedTemporal.getIP();
			int distanciaNueva = subRedTemporal.getDistancia();
			String MascaraNueva = subRedTemporal.getMascara();
			
			Subred SubRedNuevo = new Subred(IPNueva, distanciaNueva,MascaraNueva, password, IPRecibidaUDP);
			listadoSubredes.put(keys, SubRedNuevo);
		}
	}	

	/**
	 * Método que compara las tablas de reenvío de los vecinos para buscar coincidencias y redundancias.
	 * 
	 * 
	 * 
	 * @param listadoVecinos Listado con los vecinose que conoce el host.
	 * @param IP IP contenida en la trama UDP que hemos recibido de algún vecino.
	 * @param distancia Distancia a la que se encuentra en vecino.
	 * @param sigSalto Siguiente salto hacia la ruta del vecino.
	 * @param password Contraseña de autentificación en la red.
	 * @param IPLocal IP del host.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void compararTablasVecinos (TreeMap<Integer, Vecino> listadoVecinos,String IP,String distancia, String sigSalto,String password,String IPLocal){
	
		//Declaramos las variables
		Iterator itVecinos = listadoVecinos.keySet().iterator();
		TreeMap<Integer, Vecino> listaTemporal = new TreeMap();
		boolean nuevoVecinoBol=true;
		
		//Recorremos la lista de vecinos en busca de una coincidencia con el vecino recibido.
		while(itVecinos.hasNext()){
			
			Integer key =(Integer) itVecinos.next();
			Vecino vecinoTemp = listadoVecinos.get(key);
			
			String IPTemp = vecinoTemp.getIP();
			int distanciaTemp = vecinoTemp.getDistancia();
			
			//Cuando encontramos una coincidencia, comprobamos sus distancias, de recibir una distancia menor, actualizariamos las rutas.
			if(IPTemp.equals(IP)){
				
				nuevoVecinoBol=false;
				
				if((Integer.parseInt(distancia)+1)<distanciaTemp){					
						distanciaTemp=distanciaTemp+1;
						Vecino vecinoActualizado = new Vecino(IPTemp, distanciaTemp, sigSalto, password);
						
						listaTemporal.put(key, vecinoActualizado);
				}
			}
			//Si la IP recibida, coincide con nuestra IP Local, desechamos este fragmento.
			if(IP.equals(IPLocal)){
				nuevoVecinoBol=false;
			}	
		}
		//Si no se ha filtrado nada hasta ahora, significa que el vecino recibido no pertenece a la lista, entonces lo añadimos.
		if(nuevoVecinoBol){
			int nuevaKey =listadoVecinos.size()+1;
			Vecino nuevoVecino = new Vecino (IP, Integer.parseInt(distancia)+1, sigSalto, password);
			listaTemporal.put(nuevaKey, nuevoVecino);	
		}

		Iterator itTemporal = listaTemporal.keySet().iterator();
		//Actualizamos la lista de vecinos con la lista temporal que hemos usado durante el proceso.
		while(itTemporal.hasNext()){
			Integer key =(Integer) itTemporal.next();
			Vecino vecinoTemp = listaTemporal.get(key);
			
			String IPTemp = vecinoTemp.getIP();
			int distanciaTemp = vecinoTemp.getDistancia();
			String sigSaltoTemp = vecinoTemp.getSiguienteSalto();
			
			Vecino vecinoNuevo = new Vecino(IPTemp, distanciaTemp,sigSaltoTemp, password);
			listadoVecinos.put(key, vecinoNuevo);
		}		
	}

	/**Método que imprime por pantalla la tabla de vecinos del host.
	 * 
	 * 
	 * @param listadoVecinos Listado de los vecinos que conoce el host.
	 * @param IPLocal IP del host.
	 */
	public static void imprimirTablaVecinos(TreeMap<Integer, Vecino> listadoVecinos, String IPLocal){
	
		System.out.println("Listado Vecinos\nIP VECINO \tDISTANCIA\tSIGUIENTE SALTO");
	
		//Recorrido de la lista de vecinos para poder imprimirlos todos.
		Iterator<Entry<Integer, Vecino>> iterador = listadoVecinos.entrySet().iterator();
	
		while(iterador.hasNext()){
			Entry<Integer, Vecino> mapEntry = iterador.next();
			Vecino vecinoTemp = (Vecino) mapEntry.getValue();
			if(vecinoTemp.getSiguienteSalto().equals(IPLocal)){
				System.out.println(vecinoTemp.getIP() + "\t" +vecinoTemp.getDistancia() + "\t\t RUTA CONECTADA");
			}else{
				System.out.println(vecinoTemp.getIP() + "\t" +vecinoTemp.getDistancia() + "\t\t"+vecinoTemp.getSiguienteSalto());
			}
		}	
	}
	
	/**Método que imprime por pantalla la tabla de subredes que conoce el host. 
	 * 
	 * 
	 * @param listadoSubredes Listado con las subredes que conoce el host.
	 * @param IPLocal IP del host.
	 */
	public static void imprimirTablaSubredes(TreeMap<Integer, Subred> listadoSubredes, String IPLocal){
		System.out.println("\n\n\nListado Subredes\nDISTANCIA \tSIGUIENTE SALTO\t");
		
		//Recorremos la lista para inspeccionar todas las subredes.
		Iterator<Entry<Integer, Subred>> iterador = listadoSubredes.entrySet().iterator();
		
		while(iterador.hasNext()){
			Entry<Integer, Subred> mapEntry = iterador.next();
			Subred subredTemp = (Subred) mapEntry.getValue();
			
			if(subredTemp.getSiguienteSalto().equals(IPLocal)){
				System.out.println(subredTemp.getIP() + "\t" +subredTemp.getDistancia() + "\t\t Conexión directa");
			}else{
				System.out.println(subredTemp.getIP() + "\t" +subredTemp.getDistancia() + "\t\t" + subredTemp.getSiguienteSalto());
			}
			
		}
		
		System.out.println("------------------------------------------------------------");
		
	}

	/**Método que se encarga de crear un listado con los datos que conoce el host en ambas tablas de vecinos y subredes para poder comunicarse con ellos y formar así la topología de la red.
	 * 
	 * Es un mensaje importante de cara a la actualización de los datos.
	 * 
	 * 
	 * 
	 * @param listadoVecinos Listado de los vecinos que conoce el host.
	 * @param listadoSubredes Listado de las subredes que conoce el host.
	 * @param IPLocal IP del host.
	 * @param password Contraseña de autentificación de la red.
	 * @return Devuelve el mensaje a transmitir a los vecinos.
	 */
	public static String crearListaVecinos(TreeMap<Integer, Vecino> listadoVecinos, TreeMap<Integer, Subred> listadoSubredes, String IPLocal, String password){
		
		//Definimos el String
		String listaAEnviar = password + "-" + IPLocal;
		
		//Si no hay subredes, no referenciamos esta parte.
		if(!listadoSubredes.isEmpty()){
			
			@SuppressWarnings("rawtypes")
			Iterator iteradorSubredes = listadoSubredes.keySet().iterator();
			
			while(iteradorSubredes.hasNext()){
				Integer key = (Integer) iteradorSubredes.next();
				Subred subredTemp = listadoSubredes.get(key);
				
				//Añadimos cada subred al mensaje.
				listaAEnviar = listaAEnviar + "#0-" + subredTemp.getIP() + "-" + subredTemp.getMascara() + "-" + subredTemp.getDistancia();
			}
		}
		
		//Ídem al apartado anterior.
		if(!listadoVecinos.isEmpty()){

			@SuppressWarnings("rawtypes")
			Iterator iteradorVecinos = listadoVecinos.keySet().iterator();
			
			while(iteradorVecinos.hasNext()){
				Integer key = (Integer) iteradorVecinos.next();
				Vecino vecinoTemp = listadoVecinos.get(key);
				
				listaAEnviar = listaAEnviar + "#1-" + vecinoTemp.getIP() + "-" + vecinoTemp.getSiguienteSalto() + "-" + vecinoTemp.getDistancia(); 
			}		
		}
		return listaAEnviar;
	}

}