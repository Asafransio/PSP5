package simetrico;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.Timer;
public class ClienteSimetrico extends JFrame implements ActionListener
{
	private static final long serialVersionUID = 1L;
	Socket socket;
	DatagramSocket fentrada;
	DatagramSocket fsalida;
	String nombre;
	static JTextField mensaje = new JTextField();
	private JScrollPane scrollpane;
	static JTextArea textarea;
	JButton boton = new JButton("Enviar");
	JButton desconectar = new JButton("Salir");
	boolean repetir = true;
	static int puerto;
	public ClienteSimetrico(Socket socket, String nombre)
	{
		// Prepara la pantalla. Se recibe el socket creado y el nombre del cliente
		super(" Conexión del cliente chat: " + nombre);
		setLayout(null);
		mensaje.setBounds(10, 10, 400, 30);
		add(mensaje);
		textarea = new JTextArea();
		scrollpane = new JScrollPane(textarea);
		scrollpane.setBounds(10, 50, 400, 300);
		add(scrollpane);
		boton.setBounds(420, 10, 100, 30);
		add(boton);
		desconectar.setBounds(420, 50, 100, 30);
		add(desconectar);
		textarea.setEditable(false);
		boton.addActionListener(this);
		this.getRootPane().setDefaultButton(boton);
		desconectar.addActionListener(this);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.socket = socket;
		this.nombre = nombre;
		// Se crean los flujos de entrada y salida.
		// En el flujo de salida se escribe un mensaje
		// indicando que el cliente se ha unido al Chat.
		// El HiloServidor recibe este mensaje y
		// lo reenvía a todos los clientes conectados
		try
		{
			int port=2021;
			fentrada = new DatagramSocket(port);
			fsalida = new DatagramSocket();
			String texto = "SERVIDOR> Entra en el chat... " + nombre;
			
			encriptar(texto);
		}
		catch (IOException ex)
		{
			System.out.println("Error de E/S");
			ex.printStackTrace();
			System.exit(0);
		}
	}
	//El método main es el que lanza el cliente,
	//para ello en primer lugar se solicita el nombre o nick del
	//cliente, una vez especificado el nombre
	//se crea la conexión al servidor y se crear la pantalla del Chat(ClientChat)
	//lanzando su ejecución (ejecutar()).
	public static void main(String[] args) throws Exception
	{
		puerto = 44444;
		String nombre = JOptionPane.showInputDialog("Introduce tu nombre o nick:");
		Socket socket = null;
		try
		{
			socket = new Socket("localhost", puerto);
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
			JOptionPane.showMessageDialog(null, "Imposible conectar con el servidor \n" + ex.getMessage(), "<<Mensaje de Error:1>>", JOptionPane.ERROR_MESSAGE);
			System.exit(0);
		}
		if(!nombre.trim().equals(""))
		{
			ClienteSimetrico cliente = new ClienteSimetrico(socket, nombre);
			cliente.setBounds(0,0,540,400);
			cliente.setVisible(true);
			cliente.ejecutar();
		}
		else
		{
			System.out.println("El nombre está vacío...");
		}
	}
	
	
	//Cuando se pulsa el botón Enviar,
	//el mensaje introducido se envía al servidor por el flujo de salida
	public void actionPerformed(ActionEvent e)
	{
		if(e.getSource()==boton)
		{
			String texto = nombre + "> " + mensaje.getText();
			try
			{
				mensaje.setText("");
				encriptar(texto);
				//fsalida.writeUTF(texto);
				boton.setEnabled(false);
				int delay = 3000;
				Timer timer = new Timer(delay, new ActionListener() {
				    public void actionPerformed(ActionEvent evt) {
				        boton.setEnabled(true);
				    }
				});
				timer.setRepeats(false);
				timer.start();
				
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}
		// Si se pulsa el botón Salir,
		// se envía un mensaje indicando que el cliente abandona el chat
		// y también se envía un * para indicar
		// al servidor que el cliente se ha cerrado
		else if(e.getSource()==desconectar)
		{
			String texto = "SERVIDOR> Abandona el chat... " + nombre;
			try
			{
				encriptar(texto);
				encriptar("*");
				
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
			repetir = false;
			System.exit(0);
		}
	}
	// Dentro del método ejecutar(), el cliente lee lo que el
	// hilo le manda (mensajes del Chat) y lo muestra en el textarea.
	// Esto se ejecuta en un bucle del que solo se sale
	// en el momento que el cliente pulse el botón Salir
	// y se modifique la variable repetir
	public void ejecutar()
	{
		String texto = "";
		while(repetir)
		{

			try 
			{
				
				byte[] data  = new byte[1024];
				DatagramPacket packet = new DatagramPacket(data, 1024);
				fentrada.receive(packet);
				//texto = fentrada.readUTF();
				texto = desencriptar(packet);
				textarea.setText(texto);
			}
			catch (IOException ex)
			{
				JOptionPane.showMessageDialog(null, "Imposible conectar con el servidor \n" + ex.getMessage(), "<<Mensaje de Error:2>>", JOptionPane.ERROR_MESSAGE);
				repetir = false;
			}
		}
		try
		{
			socket.close();
			
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
		}
	}
	
	public void encriptar(String texto)
	{
		DatagramPacket packet = null;
		try 
		{
			int port=2022;
			byte[] ipAddr = new byte[] { 127,0,0,1 };
			InetAddress address = InetAddress.getByAddress(ipAddr);
			System.out.println(address);
			byte[] plainBytes = texto.getBytes();
			byte[] keySymme = {0x74, 0x68, 0x69, 0x73, 0x49, 0x73, 0x41, 0x53, 0x65, 0x63, 0x72, 0x65, 0x74, 0x4b, 0x65, 0x79}; // ClaveSecreta
			SecretKeySpec secretKey = new SecretKeySpec(keySymme, "AES");
			// Crear objeto Cipher e inicializar modo encriptación
			Cipher cipher = Cipher.getInstance("AES"); // Transformación
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
		System.out.println("Mensaje desencriptado del Cliente: " +texto);
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
		System.out.println("Mensaje recibido del Servidor: " + packet);
		System.out.println("Datos encriptados del Servidor: " + new String(packet.getData()));
		System.out.println("Mensaje desencriptado del Servidor: " +desencript);
		return desencript;
	}
}