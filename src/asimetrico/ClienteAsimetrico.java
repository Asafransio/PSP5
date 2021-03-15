package asimetrico;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;


import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.Timer;


public class ClienteAsimetrico extends JFrame implements ActionListener
{
	private static final long serialVersionUID = 1L;
	Socket socket;
	DataInputStream fentrada;
	DataOutputStream fsalida;
	String nombre, txtEnc, txtDes;
	static JTextField mensaje = new JTextField();
	private JScrollPane scrollpane;
	static JTextArea textarea;
	JButton boton = new JButton("Enviar");
	JButton desconectar = new JButton("Salir");
	boolean repetir = true;
	public ClienteAsimetrico(Socket socket, String nombre)
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
			fentrada = new DataInputStream(socket.getInputStream());
			fsalida = new DataOutputStream(socket.getOutputStream());
			String texto = "SERVIDOR> Entra en el chat... " + nombre;
			txtEnc = encriptar(texto);
			System.out.println("Desencriptado: "+texto);
			System.out.println("Encriptado: "+txtEnc);
			fsalida.writeUTF(txtEnc);
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
		int puerto = 44444;
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
			ClienteAsimetrico cliente = new ClienteAsimetrico(socket, nombre);
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
				txtEnc = encriptar(texto);
				System.out.println("Desencriptado: "+texto);
				System.out.println("Encriptado: "+txtEnc);
				fsalida.writeUTF(txtEnc);
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
			catch (IOException ex)
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
				txtEnc = encriptar(texto);
				fsalida.writeUTF(txtEnc);
				txtEnc = encriptar("*");
				fsalida.writeUTF(txtEnc);
				
			}
			catch (IOException ex)
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
				
				texto = fentrada.readUTF();
				txtDes = desencriptar(texto);
				textarea.setText(txtDes);
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
	public String encriptar(String texto) {
		String encriptado = "";
		
		try {
			RSA rsaCliente = new RSA();
			rsaCliente.genKeyPair(512);
			rsaCliente.saveToDiskPrivateKey("rsaCliente.pri");
			rsaCliente.saveToDiskPublicKey("rsaCliente.pub");
			encriptado = rsaCliente.Encrypt(texto);
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
			RSA rsaServidor = new RSA();
			rsaServidor.genKeyPair(512);
			rsaServidor.openFromDiskPrivateKey("rsaServidor.pri");
			rsaServidor.openFromDiskPublicKey("rsaServidor.pub");
			desencriptado = rsaServidor.Decrypt(texto);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return desencriptado;
	}
}