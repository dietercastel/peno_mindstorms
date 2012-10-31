package bluebot.ui;


import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

import bluebot.ConfigListener;
import bluebot.core.Controller;
import bluebot.core.ControllerListener;
import bluebot.graph.Tile;
import bluebot.maze.Maze;
import bluebot.maze.MazeGenerator;



/**
 * 
 * @author Ruben Feyen
 */
public class NewControllerFrame extends JFrame implements ControllerListener {
	private static final long serialVersionUID = 1L;

	private Controller controller;

	public NewControllerFrame(final Controller controller) {
		super(MainFrame.TITLE);
		this.controller = controller;

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		initComponents();
		pack();
		setLocationRelativeTo(null);
		setResizable(false);

		controller.addListener(this);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(final WindowEvent event) {
				controller.removeListener(NewControllerFrame.this);
			}
		});
	}

	private final Component createModule(final JComponent content,
			final String title) {
		content.setBorder(BorderFactory.createEtchedBorder());
		return content;
	}

	private final Component createModuleCommands() {
		final JPanel panel = new JPanel();
		
		final GridBagLayout layout = new GridBagLayout();
		layout.columnWeights = new double[] { 1D, 1D, 1D };
		layout.rowHeights = new int[] { 64 };
		layout.rowWeights = new double[] { 1D };
		panel.setLayout(layout);
		
		final Font font = new Font(Font.SANS_SERIF, Font.BOLD, 12);
		
		final JButton buttonCalibrate = new JButton("Calibrate");
		buttonCalibrate.setFocusable(false);
		buttonCalibrate.setFont(font);
		buttonCalibrate.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				controller.doCalibrate();
			}
		});
		
		final JButton buttonOrientate = new JButton("Orientate");
		buttonOrientate.setFocusable(false);
		buttonOrientate.setFont(font);
		buttonOrientate.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				controller.doWhiteLineOrientation();
			}
		});
		
		final JButton buttonPolygon = new JButton("Polygon");
		buttonPolygon.setFocusable(false);
		buttonPolygon.setFont(font);
		buttonPolygon.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				final PolygonDialog dialog = new PolygonDialog();
				if (dialog.confirm()) {
					controller.doPolygon(dialog.getCorners(),
							dialog.getLength());
				}
			}
		});
		
		final GridBagConstraints gbc = SwingUtils.createGBC();
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets.set(2, 2, 2, 2);
		
		gbc.gridx = 0;
		gbc.gridy = 0;
		panel.add(buttonCalibrate, gbc);
		
		gbc.gridx++;
		panel.add(buttonOrientate, gbc);
		
		gbc.gridx++;
		panel.add(buttonPolygon, gbc);
		
		return createModule(panel, "Commands");
	}

	private final Component createModuleCommunication() {
		final CommunicationTable table = new CommunicationTable();
		controller.addListener(table.getModel());

		final JScrollPane scroll = table.createScrollPane();
		scroll.setMinimumSize(new Dimension(1, 1));
		return createModule(scroll, "Communication");
	}

	private final Component createModuleControls() {
		final GaugeComponent speed = new GaugeComponent(controller);
		
		final JoystickComponent joystick = new JoystickComponent(controller);
		joystick.setEnabled(false);
		
		final GridBagLayout layout = new GridBagLayout();
		layout.columnWeights = new double[] { 0D, 0D };
		layout.rowWeights = new double[] { 0D };
		
		final JPanel panel = new JPanel(layout);
		
		final GridBagConstraints gbc = SwingUtils.createGBC();
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets.set(5, 5, 5, 5);
		
		gbc.gridx = 0;
		gbc.gridy = 0;
		panel.add(speed, gbc);
		
		gbc.gridx++;
		panel.add(joystick, gbc);
		
		speed.addListener(new GaugeListener() {
			public void onValueChanged(final int value) {
				joystick.setEnabled(value > 0);
			}
		});
		controller.addListener(new ConfigListener() {
			public void onSpeedHigh() {
				speed.setValue(100);
			}
			
			public void onSpeedLow() {
				speed.setValue(30);
			}

			public void onSpeedMedium() {
				speed.setValue(70);
			}
		});
		
		return createModule(panel, "Controls");
	}

	private final Component createModuleRenderer() {
		final VisualizationComponent canvas = new VisualizationComponent();
		// TODO: Remove after debugging
		canvas.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(final MouseEvent event) {
				canvas.removeMouseListener(this);
				
				/*
				final Tile tile = new Tile(0, 0);
				tile.setBorderNorth(Border.OPEN);
				tile.setBorderEast(Border.CLOSED);
				tile.setBorderSouth(Border.CLOSED);
				tile.setBorderWest(Border.CLOSED);
				canvas.onTileUpdate(tile);
				*/
				
				final Maze maze = new MazeGenerator().generateMaze();
				for (final Tile tile : maze.getTiles()) {
					canvas.onTileUpdate(tile);
				}
				
				/*
				final Thread thread = new Thread(new Runnable() {
					public void run() {
						float heading = 0F;
						float x = 0F;
						float y = 800F;
						
						final float speed = 14F;
						
						for (;; heading += 1F) {
							heading = Utils.clampAngleDegrees(heading);
							
							x += (speed * Math.sin(heading * Math.PI / 180D));
							y += (speed * Math.cos(heading * Math.PI / 180D));
							
							canvas.onMotion(x, y, heading);
							
							try {
								Thread.sleep(25);
							} catch (final InterruptedException e) {
								break;
							}
						}
					}
				});
				thread.setDaemon(true);
				thread.start();
				
				canvas.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseClicked(final MouseEvent event) {
						thread.interrupt();
					}
				});
				*/
			}
		});
		controller.addListener(canvas);
		return createModule(canvas, "Visualization");
	}
	
	private final Component createModuleSensors() {
		final SensorsComponent sensors = new SensorsComponent();
		controller.addListener(sensors);
		return createModule(sensors, "Sensors");
	}
	
	private final JTabbedPane createTabs() {
		final JTabbedPane tabs = new JTabbedPane(JTabbedPane.TOP);
		tabs.addTab("Communication", createModuleCommunication());
		tabs.addTab("Visualization", createModuleRenderer());
		tabs.setSelectedIndex(1);
		return tabs;
	}

	private final void initComponents() {
		final GridBagLayout layout = new GridBagLayout();
		layout.columnWeights = new double[] { 0D, 0D };
		layout.rowWeights = new double[] { 0D, 0D, 1D };
		setLayout(layout);
		
		final GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets.set(5, 5, 5, 5);
		
		gbc.gridheight = 3;
		
		gbc.gridx = 0;
		gbc.gridy = 0;
		add(createTabs(), gbc);
		
		gbc.gridheight = 1;
		
		gbc.gridx++;
		add(createModuleControls(), gbc);
		
		gbc.gridy++;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		add(createModuleCommands(), gbc);
		
		gbc.gridy++;
		gbc.fill = GridBagConstraints.BOTH;
		add(createModuleSensors(), gbc);
	}

	public void onError(final String msg) {
		JOptionPane.showMessageDialog(this, msg, "Error",
				JOptionPane.ERROR_MESSAGE);
	}
	
	public void onMessage(final String msg, final String title) {
		JOptionPane.showMessageDialog(this, msg, title,
				JOptionPane.INFORMATION_MESSAGE);
	}
	
}