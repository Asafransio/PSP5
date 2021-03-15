package simetrico;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class HiloServidorSimetrico extends Thread
{
	DatagramSocket fentrada;
	Socket socket;
	static boolean fin = false;
	DatagramSocket fsalida;
	Random rng = new Random();
	int puerto = 44444;

	public HiloServidorSimetrico(Socket socket)
	{
		this.socket = socket;
		
		try
		{
			int port = 2022;
			fentrada = new DatagramSocket(port);
			fsalida = new DatagramSocket();
		}
		catch (IOException e)
		{
			System.out.println("Error de E/S");
			e.printStackTrace();
		}
	}
	
	// En el m�todo run() lo primero que hacemos
	// es enviar todos los mensajes actuales al cliente que se
	// acaba de incorporar
	public void run()
	{
		ServidorSimetrico.mensaje.setText("N�mero de conexiones actuales: " +
				ServidorSimetrico.ACTUALES);
		String texto = ServidorSimetrico.textarea.getText();
		EnviarMensajes(texto);
		// Seguidamente, se crea un bucle en el que se recibe lo que el cliente escribe en el chat.
		// Cuando un cliente finaliza con el bot�n Salir, se env�a un * al servidor del Chat,
		// entonces se sale del bucle while, ya que termina el proceso del cliente,
		// de esta manera se controlan las conexiones actuales
		while(!fin)
		{
			String cadena = "";
			try
			{
				byte [] b = new byte [1024];
				DatagramPacket packet = new DatagramPacket(b, 1024);
				fentrada.receive(packet);
				cadena=desencriptar(packet);
				if(cadena.trim().equals("*"))
				{
					ServidorSimetrico.ACTUALES--;
					ServidorSimetrico.mensaje.setText("N�mero de conexiones actuales: "
							+ ServidorSimetrico.ACTUALES);
					fin=true;
					socket.close();
				}
				// El texto que el cliente escribe en el chat,
				// se a�ade al textarea del servidor y se reenv�a a todos los clientes
				else
				{
					ServidorSimetrico.textarea.append(cadena + "\n");
					
					if(fin!=true) {
						try {
							int num = Integer.parseInt(cadena.split("> ")[1]);
							String jugador = cadena.split("> ")[0];
							if (num == ServidorSimetrico.premio) {
								ServidorSimetrico.textarea.append("SERVIDOR> " + jugador + 
										" piensa que el n�mero es el " + num + ". "
										+ "\nSERVIDOR> Y HA ACERTADOOOO!!!!" 
										+ "\nSERVIDOR> Fin de la partida. \n");
								
								EnviarMensajes(texto);
								
								fin = true;
							} else if (num > 100 || num < 1) {
								texto="SERVIDOR> " + jugador + " piensa que el n�mero es el " 
										+ num + ".\nSERVIDOR> El n�mero puede ser cualquiera entra 1 y 100.\n";
								ServidorSimetrico.textarea.append(texto);
								EnviarMensajes(texto);
								
							} else if (num > ServidorSimetrico.premio) {
								texto="SERVIDOR> " + jugador + " piensa que el n�mero es el " 
										+ num + ".\nSERVIDOR> El n�mero es menor a " + num + ".\n";
								ServidorSimetrico.textarea.append(texto);
								EnviarMensajes(texto);
							} else if (num < ServidorSimetrico.premio) {
								texto="SERVIDOR> " + jugador + " piensa que el n�mero es el " 
										+ num + ".\nSERVIDOR> El n�mero es mayor a " + num + ".\n";
								ServidorSimetrico.textarea.append(texto);
								EnviarMensajes(texto);
							}
						} catch (NumberFormatException excepcion) {
							if(cadena.contains("SERVIDOR>")==false) {
								ServidorSimetrico.textarea.append("SERVIDOR> ���ERROR!!! S�lo es posible introducir n�meros.\n");
							}
						}
						texto = ServidorSimetrico.textarea.getText();
						EnviarMensajes(texto);
				}
			}
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
				fin=true;
			}
		}
	
	}
	// El m�todo EnviarMensajes() env�a el texto del textarea a
	// todos los sockets que est�n en la tabla de sockets,
	// de esta forma todos ven la conversaci�n.
	// El programa abre un stream de salida para escribir el texto en el socket
	private void EnviarMensajes(String texto)
	{
		for(int i=0; i<ServidorSimetrico.CONEXIONES; i++)
		{
			try
			{
				
				encriptar(texto);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public void encriptar(String texto)
	{
		DatagramPacket packet = null;
		try 
		{
			int port=2021;
			byte[] ipAddr = new byte[] { 127,0,0,1 };
			InetAddress address = InetAddress.getByAddress(ipAddr);
			byte[] plainBytes = texto.getBytes();
			byte[] keySymme = {0x74, 0x68, 0x69, 0x73, 0x49, 0x73, 0x41, 0x53, 0x65, 0x63, 0x72, 0x65, 0x74, 0x4b, 0x65, 0x79}; // ClaveSecreta
			SecretKeySpec secretKey = new SecretKeySpec(keySymme, "AES");
			// Crear objeto Cipher e inicializar modo encriptaci�n
			Cipher cipher = Cipher.getInstance("AES"); // Transformaci�n
			cipher.init(Cipher.ENCRYPT_MODE, secretKey);
			byte[] EncryptedData = cipher.doFinal(plainBytes);	
			packet = new DatagramPacket(EncryptedData, EncryptedData.length, address, port);
			fsalida.send(packet);
		} 
		catch (Exception e) 
		{
			// TODO: handle exception
			e.printStackTrace();
		}
		
		System.out.println("Enviando mensaje al Servidor: " + packet);
		System.out.println("Datos encriptados del Cliente: " + new String(packet.getData()));
		System.out.println("Mensaje desencriptado Cliente: " +texto);
		//return packet;
	}
	private String desencriptar(DatagramPacket packet)
	{
		
		String desencript = "";
		
		byte[] keySymme = {0x74, 0x68, 0x69, 0x73, 0x49, 0x73, 0x41, 0x53, 0x65, 0x63, 0x72, 0x65, 0x74, 0x4b, 0x65, 0x79}; // ClaveSecreta
		SecretKeySpec secretKey = new SecretKeySpec(keySymme, "AES");
		try
		{
			Cipher cipher = Cipher.getInstance("AES");
			// Reiniciar Cipher al modo desencriptado
			cipher.init(Cipher.DECRYPT_MODE, secretKey, cipher.getParameters());
			byte[] plainBytesDecrypted = cipher.doFinal(packet.getData(),packet.getOffset(), packet.getLength());
			desencript = new String(plainBytesDecrypted);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		System.out.println("SERVIDOR=> Mensaje recibido del Cliente: " + packet);
		System.out.println("SERVIDOR=> Datos encriptado del Cliente: " + new String(packet.getData()));
		System.out.println("SERVIDOR=> Mensaje desencriptado del Cliente: " +desencript);
		return desencript;
	}
}