package asimetrico;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import java.util.Random;
import java.util.Vector;



public class HiloServidorAsimetrico extends Thread
{
	DataInputStream fentrada;
	Socket socket;
	static boolean fin = false;
	DataOutputStream fsalida;
	Random rng = new Random();
	String txtEnc, txtDes;
	Vector<String> lineas = new Vector<String>();

	public HiloServidorAsimetrico(Socket socket)
	{
		this.socket = socket;
		try
		{
			fentrada = new DataInputStream(socket.getInputStream());
			fsalida = new DataOutputStream(socket.getOutputStream());
		}
		catch (IOException e)
		{
			System.out.println("Error de E/S");
			e.printStackTrace();
		}
	}
	// En el método run() lo primero que hacemos
	// es enviar todos los mensajes actuales al cliente que se
	// acaba de incorporar
	public void run()
	{
		ServidorAsimetrico.mensaje.setText("Número de conexiones actuales: " +
				ServidorAsimetrico.ACTUALES);
		String texto = ServidorAsimetrico.textarea.getText();
		txtEnc = encriptar(texto);
		EnviarMensajes(txtEnc);
		// Seguidamente, se crea un bucle en el que se recibe lo que el cliente escribe en el chat.
		// Cuando un cliente finaliza con el botón Salir, se envía un * al servidor del Chat,
		// entonces se sale del bucle while, ya que termina el proceso del cliente,
		// de esta manera se controlan las conexiones actuales
		while(!fin)
		{
			String cadena = "";
			try
			{
				cadena = fentrada.readUTF();
				txtDes = desencriptar(cadena);
				
				System.out.println("Encriptado: "+cadena);
				System.out.println("Desencriptado: "+txtDes);
				
				if(txtDes.trim().equals("*"))
				{
					ServidorAsimetrico.ACTUALES--;
					ServidorAsimetrico.mensaje.setText("Número de conexiones actuales: "
							+ ServidorAsimetrico.ACTUALES);
					fin=true;
					socket.close();
				}
				// El texto que el cliente escribe en el chat,
				// se añade al textarea del servidor y se reenvía a todos los clientes
				else
				{
					ServidorAsimetrico.textarea.append(txtDes + "\n");
					
					if(fin!=true) {
						try {
							int num = Integer.parseInt(txtDes.split("> ")[1]);
							String jugador = txtDes.split("> ")[0];
							if (num == ServidorAsimetrico.premio) {
								ServidorAsimetrico.textarea.append("SERVIDOR> " + jugador + 
										" piensa que el número es el " + num + ". "
										+ "\nSERVIDOR> Y HA ACERTADOOOO!!!!" 
										+ "\nSERVIDOR> Fin de la partida. \n");
								
								EnviarMensajes(texto);
								
								fin = true;
							} else if (num > 100 || num < 1) {
								texto="SERVIDOR> " + jugador + " piensa que el número es el " 
										+ num + ".\nSERVIDOR> El número puede ser cualquiera entra 1 y 100.\n";
								ServidorAsimetrico.textarea.append(texto);
								EnviarMensajes(texto);
								
							} else if (num > ServidorAsimetrico.premio) {
								texto="SERVIDOR> " + jugador + " piensa que el número es el " 
										+ num + ".\nSERVIDOR> El número es menor a " + num + ".\n";
								ServidorAsimetrico.textarea.append(texto);
								EnviarMensajes(texto);
							} else if (num < ServidorAsimetrico.premio) {
								texto="SERVIDOR> " + jugador + " piensa que el número es el " 
										+ num + ".\nSERVIDOR> El número es mayor a " + num + ".\n";
								ServidorAsimetrico.textarea.append(texto);
								EnviarMensajes(texto);
							}
						} catch (NumberFormatException excepcion) {
							if(cadena.contains("SERVIDOR>")==false) {
								ServidorAsimetrico.textarea.append("SERVIDOR> ¡¡¡ERROR!!! Sólo es posible introducir números.\n");
							}
						}
						String [] parrafos = ServidorAsimetrico.textarea.getText().split("\n");
						for(int i=0;i<parrafos.length;i++){
							 String linea = parrafos[i];
							 lineas.add(linea);
						}
						if(lineas.lastElement() != null) {
							EnviarMensajes(lineas.lastElement());
						}
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
	// El método EnviarMensajes() envía el texto del textarea a
	// todos los sockets que están en la tabla de sockets,
	// de esta forma todos ven la conversación.
	// El programa abre un stream de salida para escribir el texto en el socket
	private void EnviarMensajes(String texto)
	{
		for(int i=0; i<ServidorAsimetrico.CONEXIONES; i++)
		{
			Socket socket = ServidorAsimetrico.tabla[i];
			try
			{
				fsalida = new DataOutputStream(socket.getOutputStream());
				txtEnc = encriptar(texto);
				fsalida.writeUTF(txtEnc);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public String encriptar(String texto) {
		String encriptado = "";
		// Trabajamos con las claves privadas y públicas
		try {
			RSA rsaServidor = new RSA();
			rsaServidor.genKeyPair(512);
			rsaServidor.saveToDiskPrivateKey("rsaServidor.pri");
			rsaServidor.saveToDiskPublicKey("rsaServidor.pub");
			encriptado = rsaServidor.Encrypt(texto);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return encriptado;
	}
	public String desencriptar(String texto) {
		String desencriptado = "";
		// Trabajamos con las claves privadas y públicas
		try {
			RSA rsaCliente = new RSA();
			rsaCliente.genKeyPair(512);
			rsaCliente.openFromDiskPrivateKey("rsaCliente.pri");
			rsaCliente.openFromDiskPublicKey("rsaCliente.pub");
			desencriptado = rsaCliente.Decrypt(texto);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return desencriptado;
	}
}