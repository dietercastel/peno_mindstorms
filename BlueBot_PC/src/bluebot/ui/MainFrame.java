package bluebot.ui;


import static bluebot.core.ControllerFactory.getControllerFactory;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import lejos.pc.comm.NXTCommException;

import bluebot.core.Controller;



/**
 * 
 * @author Ruben Feyen
 */
public class MainFrame extends JFrame {
	private static final long serialVersionUID = 1L;
	
	public static final String DEFAULT_BRICK_NAME = "BlueBot";
	public static final String TITLE = "P&O BlueBot";
	
	
	public MainFrame() {
		super(TITLE);
		initComponents();
		pack();
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setResizable(false);
		setLocationRelativeTo(null);
	}
	
	
	
	private final void connectToBrick() {
		final String name = JOptionPane.showInputDialog("What is the name of the NXT brick?", DEFAULT_BRICK_NAME);
		if ((name != null) && !name.isEmpty()) {
			connectToBrick(name);
		}
	}
	
	private final void connectToBrick(final String name) {
		try {
			showController(getControllerFactory().connectToBrick(name));
		} catch (final NXTCommException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this,
					e.getMessage(),
					"Connection failed",
					JOptionPane.ERROR_MESSAGE);
			return;
		}
	}
	
	private final void connectToSimulator() {
		showController(getControllerFactory().connectToSimulator());
	}
	
	private static final JButton createButton(final String text) {
		final JButton button = new JButton(text);
		button.setPreferredSize(new Dimension(256, 64));
		return button;
	}
	
	private final void initComponents() {
		setLayout(new BorderLayout(0, 0));
		
		final JButton btnBrick = createButton("Connect to NXT brick");
		btnBrick.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				connectToBrick();
			}
		});
		
		final JButton btnSim = createButton("Connect to simulator");
		btnSim.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				connectToSimulator();
			}
		});
		
		add(btnBrick, BorderLayout.NORTH);
		add(btnSim, BorderLayout.SOUTH);
	}
	
	private final void showController(final Controller controller) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				final JFrame frame = new ControllerFrame(controller);
				frame.setVisible(true);
			}
		});
	}
	
}