package cleode;

//============================================================================
// Name        : TestZCL.java
// Author      : flr
// Version     : $Revision: 1.11 $
// Copyright   : (C) 2003-2012 by CLEODE SA
// Description : Test ZCL
//============================================================================
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.cleode.zcl.AttrConstants;
import com.cleode.zcl.Attribute;
import com.cleode.zcl.ClusterName;
import com.cleode.zcl.ClusterSpecCmdAttribute;
import com.cleode.zcl.DataType;
import com.cleode.zcl.Direction;
import com.cleode.zcl.DiscoveredAttribute;
import com.cleode.zcl.FrameType;
import com.cleode.zcl.ManufacturerCode;
import com.cleode.zcl.ReadAttributeResponse;
import com.cleode.zcl.ReadAttributesPayload;
import com.cleode.zcl.ReadReportingConfigAttribute;
import com.cleode.zcl.ReadReportingConfigAttributeResponse;
import com.cleode.zcl.ReportAttribute;
import com.cleode.zcl.ReportingConfigurationAttribute;
import com.cleode.zcl.ReportingConfigurationAttributeResponse;
import com.cleode.zcl.ReportingDirection;
import com.cleode.zcl.ResponseStatus;
import com.cleode.zcl.Status;
import com.cleode.zcl.WriteAttribute;
import com.cleode.zcl.WriteAttributeResponse;
import com.cleode.zcl.ZCLFrame;
import com.cleode.zigbee.Binding;
import com.cleode.zigbee.Cluster;
import com.cleode.zigbee.EndPoint;
import com.cleode.zigbee.IEEEAddress;
import com.cleode.zigbee.Link;
import com.cleode.zigbee.Node;
import com.cleode.zigbeeconnector.AliveCallback;
import com.cleode.zigbeeconnector.BindCmd;
import com.cleode.zigbeeconnector.BindResponse;
import com.cleode.zigbeeconnector.LoadingProgressCallback;
import com.cleode.zigbeeconnector.Reason;
import com.cleode.zigbeeconnector.ResponseCallBack;
import com.cleode.zigbeeconnector.ZigbeeAddressResolvingServices;
import com.cleode.zigbeeconnector.ZigbeeAuthorizationServices;
import com.cleode.zigbeeconnector.ZigbeeConnector;
import com.cleode.zigbeeconnector.ZigbeeConnectorEvents;
import com.cleode.zigbeeconnector.ZigbeeConnectorEventsSubscribe;
import com.cleode.zigbeeconnector.ZigbeeConnectorNodeEvents;
import com.cleode.zigbeeconnector.ZigbeeConnectorServices;

import fr.lab.lissi.general.Constants;

//import com.opencsv.CSVWriter;

public class TestZCL implements AliveCallback, ZigbeeConnectorNodeEvents,
		ZigbeeConnectorEvents, LoadingProgressCallback {
	protected final SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");

	protected final ZigbeeConnectorServices zcs = ZigbeeConnector.singleton
			.getZigbeeConnectorServices();
	protected final ZigbeeConnectorEventsSubscribe zces = ZigbeeConnector.singleton
			.getZigbeeConnectorEventsSubscribe();
	protected final ZigbeeAuthorizationServices zas = ZigbeeConnector.singleton
			.getZigbeeAuthorizationServices();
	protected final ZigbeeAddressResolvingServices zars = ZigbeeConnector.singleton
			.getZigbeeAddressResolvingServices();

	protected final Map<Integer, Node> mapOfNodes = Collections
			.synchronizedMap(new HashMap<Integer, Node>());
	protected final Map<Integer, String> addressOfNodes = Collections
			.synchronizedMap(new HashMap<Integer, String>());

	protected Date date;
	SimpleDateFormat ft = new SimpleDateFormat("E_dd.MM.yyyy_hh.mm.ss");
	SimpleDateFormat timeCell = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");

	protected ZigbeeConnectorEvents observer = new ZigbeeConnectorEvents() {
		
//		public static TestZCL getInstance() {
//			return SingletonHolder.INSTANCE;
//		}
//
//		private static class SingletonHolder {
//			private final static TestZCL INSTANCE = new TestZCL();
//		}
		@Override
		public void notifyReportAttribute(int nwkAddr, int epNumber,
				int clusterId, int seqNumber, final ReportAttribute[] attributes) {
			try {
				displayReportAttribute("Two", nwkAddr, epNumber, clusterId,
						attributes);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		@Override
		public void notifyClusterSpecificCommand(int nwkAddr, int epNumber,
				int clusterId, int seqNumber, byte command,
				ClusterSpecCmdAttribute[] attributes) {
			displayClusterSpecificCommand("Two", nwkAddr, epNumber, clusterId,
					command, attributes);
		}
	};

	/**
	 * Search a node from its network address, or null if not found.
	 * 
	 * @param nwkAddr
	 *            the network address.
	 * @return the node, or null if not found.
	 */
	public Node searchNode(int nwkAddr) {
		return this.mapOfNodes.get(nwkAddr);
	}

	/**
	 * Search a node from its IEEE address, or null if not found.
	 * 
	 * @param ieeeAddr
	 *            the IEEE address.
	 * @return the node, or null if not found.
	 */
	public Node searchNode(final IEEEAddress ieeeAddr) {
		if (ieeeAddr.equals(IEEEAddress.NULL_IEEE_ADDR) != true
				&& ieeeAddr.equals(IEEEAddress.UNKNOWN_IEEE_ADDR) != true) {
			synchronized (this.mapOfNodes) {
				for (Node node : this.mapOfNodes.values()) {
					if (node.getIEEEAddress().equals(ieeeAddr)) {
						return node;
					}
				}
			}
		}
		return null;
	}

	/**
	 * Search a node from its model identifier, or null if not found.
	 * 
	 * @param modelId
	 *            the model identifier
	 * @return the first node with the model identifier, or null if not found.
	 */
	public Node searchNode(final String modelId) {
		synchronized (this.mapOfNodes) {
			for (Node node : this.mapOfNodes.values()) {
				if (modelId.equals(node.getModelIdentifier())) {
					return node;
				}
			}
		}
		return null;
	}

	protected ResponseCallBack<ZCLFrame> zclRespCallback = new ResponseCallBack<ZCLFrame>() {
		@Override
		public void responseReceived(int nwkAddr, int epNumber, int clusterId,
				int seqNumber, final ZCLFrame response) {
			System.out
					.printf("=====> responseReceived ZCLFrame from 0x%04x Ep(%d) Cl(0x%04x) : %s \n",
							nwkAddr, epNumber, clusterId,
							(response == null) ? "Timeout expired !" : response
									.getHeader().getCmdIdentifier().name());
		}
	};

	protected ResponseCallBack<ReadAttributeResponse[]> zclReader = new ResponseCallBack<ReadAttributeResponse[]>() {
		@Override
		public void responseReceived(int nwkAddr, int epNumber, int clusterId,
				int seqNumber, final ReadAttributeResponse[] responses) {
			System.out
					.printf("=====> responseReceived ReadAttributeResponse from 0x%04x Ep(%d) Cl(0x%04x) : %s \n",
							nwkAddr,
							epNumber,
							clusterId,
							(responses == null) ? "Timeout expired !" : Integer
									.toString(responses.length));
			if (responses != null) {
				for (ReadAttributeResponse attr : responses) {
					String dataAsString = "To be completed";
					if (attr.getStatus() == Status.SUCCESS) {
						if (attr.getDataType() == DataType.CHARACTER_STRING) {
							dataAsString = attr.getDataAsString();
						} else if (attr.getDataType().isBoolean()) {
							dataAsString = Boolean.toString(attr
									.getDataAsBoolean());
						} else if (attr.getDataType().isNumber()) {
							dataAsString = attr.getDataAsNumber().toString();
						}
						System.out
								.printf("AttrId(0x%04x) - Status(%s) - DataType(%s) : %s\n",
										attr.getIdentifier(), attr.getStatus(),
										attr.getDataType(), dataAsString);
						addressOfNodes.put(nwkAddr, dataAsString);
					} else {
						System.out.printf("AttrId(0x%04x) - Status(%s) \n",
								attr.getIdentifier(), attr.getStatus());
					}
				}
			}
			System.out.println();
		}
	};

	protected ResponseCallBack<WriteAttributeResponse[]> zclWriter = new ResponseCallBack<WriteAttributeResponse[]>() {
		@Override
		public void responseReceived(int nwkAddr, int epNumber, int clusterId,
				int seqNumber, final WriteAttributeResponse[] responses) {
			System.out
					.printf("=====> responseReceived WriteAttributeResponse from 0x%04x Ep(%d) Cl(0x%04x) : %s \n",
							nwkAddr,
							epNumber,
							clusterId,
							(responses == null) ? "Timeout expired !" : Integer
									.toString(responses.length));
			if (responses != null) {
				for (WriteAttributeResponse attr : responses) {
					System.out.printf("AttrId(0x%04x) - Status(%s)\n",
							attr.getIdentifier(), attr.getStatus());
				}
			}
			System.out.println();
		}
	};

	protected ResponseCallBack<DiscoveredAttribute[]> zclDiscover = new ResponseCallBack<DiscoveredAttribute[]>() {
		@Override
		public void responseReceived(int nwkAddr, int epNumber, int clusterId,
				int seqNumber, final DiscoveredAttribute[] responses) {
			System.out
					.printf("=====> responseReceived DiscoveredAttribute from 0x%04x Ep(%d) Cl(0x%04x) : %s \n",
							nwkAddr,
							epNumber,
							clusterId,
							(responses == null) ? "Timeout expired !" : Integer
									.toString(responses.length));
			if (responses != null) {
				for (DiscoveredAttribute attr : responses) {
					System.out.printf("AttrId(0x%04x) - DataType(%s)\n",
							attr.getIdentifier(), attr.getDataType());
				}
			}
			System.out.println();
		}
	};

	protected ResponseCallBack<ReadReportingConfigAttributeResponse[]> zclReportReader = new ResponseCallBack<ReadReportingConfigAttributeResponse[]>() {
		@Override
		public void responseReceived(int nwkAddr, int epNumber, int clusterId,
				int seqNumber,
				final ReadReportingConfigAttributeResponse[] responses) {
			System.out
					.printf("=====> responseReceived ReadReportingConfigAttributeResponse from 0x%04x Ep(%d) Cl(0x%04x) : %s \n",
							nwkAddr,
							epNumber,
							clusterId,
							(responses == null) ? "Timeout expired !" : Integer
									.toString(responses.length));
			if (responses != null) {
				for (ReadReportingConfigAttributeResponse attr : responses) {
					if (attr.getStatus() == Status.SUCCESS) {
						String reportChange = (attr.getDataType().isAnalog() ? String
								.format("- Change(%d)", attr
										.getReportableChangeAsNumber()
										.longValue()) : "");
						System.out
								.printf("AttrId(0x%04x) - Direction(%s) - Status(%s) DataType(%s) - Min(%d) - Max(%d) %s\n",
										attr.getIdentifier(),
										attr.getDirection(), attr.getStatus(),
										attr.getDataType(),
										attr.getMinReportingInterval(),
										attr.getMaxReportingInterval(),
										reportChange);
					} else {
						System.out
								.printf("AttrId(0x%04x) - Direction(%s) - Status(%s)\n",
										attr.getIdentifier(),
										attr.getDirection(), attr.getStatus());
					}
				}
			}
			System.out.println();
		}
	};

	protected ResponseCallBack<ReportingConfigurationAttributeResponse[]> zclReportWriter = new ResponseCallBack<ReportingConfigurationAttributeResponse[]>() {
		@Override
		public void responseReceived(int nwkAddr, int epNumber, int clusterId,
				int seqNumber,
				final ReportingConfigurationAttributeResponse[] responses) {
			System.out
					.printf("=====> responseReceived ReportingConfigurationAttributeResponse from 0x%04x Ep(%d) Cl(0x%04x) : %s \n",
							nwkAddr,
							epNumber,
							clusterId,
							(responses == null) ? "Timeout expired !" : Integer
									.toString(responses.length));
			if (responses != null) {
				for (ReportingConfigurationAttributeResponse attr : responses) {
					System.out.printf(
							"AttrId(0x%04x) - Direction(%s) - Status(%s)\n",
							attr.getIdentifier(), attr.getDirection(),
							attr.getStatus());
				}
			}
			System.out.println();
		}
	};

	protected ResponseCallBack<ClusterSpecCmdAttribute[]> zclClSpecCmdReader = new ResponseCallBack<ClusterSpecCmdAttribute[]>() {
		@Override
		public void responseReceived(int nwkAddr, int epNumber, int clusterId,
				int seqNumber, ClusterSpecCmdAttribute[] responses) {
			System.out
					.printf("=====> responseReceived ClusterSpecCmdAttribute from 0x%04x Ep(%d) Cl(0x%04x) : %s\n",
							nwkAddr,
							epNumber,
							clusterId,
							(responses == null) ? "Timeout expired !" : Integer
									.toString(responses.length));
			if (responses != null) {
				for (ClusterSpecCmdAttribute attr : responses) {
					String str;
					if (attr.getDataType().isNumber()) {
						str = attr.getDataAsNumber().toString();
					} else if (attr.getDataType().isString()) {
						str = attr.getDataAsString();
					} else if (attr.getDataType().isBoolean()) {
						str = Boolean.toString(attr.getDataAsBoolean());
					} else {
						str = Arrays.toString(attr.getData());
					}
					System.out.printf("Position(%d) - DataType(%s) : %s \n",
							attr.getPosition(), attr.getDataType(), str);
				}
			}
			System.out.println();
		}
	};

	protected BindResponse zclBinder = new BindResponse() {
		@Override
		public void responseReceived(int nwkAddr, final BindCmd cmd,
				final Status status) {
			System.out
					.printf("=====> responseReceived %s Response from 0x%04x : Status(%s)  \n",
							cmd, nwkAddr, status);
			if (status == Status.SUCCESS) {
				Node node = searchNode(nwkAddr);
				if (node != null) {
					for (Binding binding : node.getBindings()) {
						displayBinding(node, binding);
					}
				}
			}
		}
	};

	private static long energy;

	public TestZCL() {
		boolean debug = false;
		
		Properties properties = new Properties();
		try {
			properties.load(new FileInputStream(Constants.CONFIG_FILE_PATH));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		// Set the license key by programmation
		this.zcs.setLicenseKey( properties.getProperty("licenceKey") );

		// Subscribe to receive node events
		// this.zcs.subscribe(this);

		// Subscribe to receive notification events
		this.zces.subscribe(this, ZigbeeConnectorEventsSubscribe.allNodes);

		// Start the communication with UBee
		boolean result = this.zcs.startUbee(this);
		System.out.println("StartUbee = " + Boolean.valueOf(result).toString());

		InputStreamReader in = new InputStreamReader(System.in);
		BufferedReader reader = new BufferedReader(in);
		String line = null;
		System.out.println("Tap 'q' to quit the program, 'h' for help");
		do {
			try {
				line = reader.readLine();
				if (line.equals("h")) {
					System.out.println("'a' Load UBee firmware");
					System.out
							.println("'b' Read Attributes command from ZPLUG");
					System.out
							.println("'c' Configure Reporting command to ZRC and ZPLUG");
					System.out
							.println("'d' Discover Attributes command from ZPLUG");
					System.out
							.println("'e' Bind On/Off cluster : ZRC ==> ZPLUG");
					System.out
							.println("'f' Unbind On/Off cluster : ZRC ==> ZPLUG");
					System.out.println("'g' Refresh the bindings of all nodes");
					System.out.println("'h' Print this help message");
					System.out.println("'i' Print the bindings of all nodes");
					System.out.println("'j' Print the searched node (ZPLUG)");
					System.out.println("'k' Print the count of nodes");
					System.out.println("'l' Print the list of nodes");
					System.out
							.println("'m' Print the list of nodes with clusters");
					System.out.println("'n' Subscribe an Observer to ZRC");
					System.out.println("'o' Subscribe an Observer to ZPLUG");
					System.out
							.println("'p' Read Reporting Configuration command from ZRC and ZPLUG");
					System.out.println("'q' Quit the program");
					System.out
							.println("'r' Read Attributes command from ZPLUG");
					System.out
							.println("'s' Specific Cluster command without response to ZPLUG");
					System.out.println("'t' Set Debug trace On/Off");
					System.out
							.println("'u' Specific Cluster command with response to ZPLUG");
					System.out.println("'v' Print the ZCL library version");
					System.out.println("'w' Write Attributes command to ZPLUG");
					System.out.println("'x' Stop and Start UBEE");
					System.out
							.println("'y' Force the ZPLUG to leave the network");
					System.out.println("'z' ZCL frame command");
					System.out
							.println("'A' Print the list of authorized devices");
					System.out
							.println("'B' Add the discovered nodes in the list of authorized devices");
					System.out
							.println("'C' Clear the list of authorized devices");
					System.out
							.println("'D' Remove the ZPlug from the list of authorized devices");
					System.out
							.println("'E' Stop UBee, Initialize the authorized devices and Start UBee ");
					System.out
							.println("'F' Configure the network channel = 20 and the Pan ID = 0x2222 ");
					System.out
							.println("'G' Print the network channel and a Pan ID");
					System.out.println("'O' Open association to the network");
				} else if (line.equals("a")) {
					System.out.print("Tap the filename to be loaded : ");
					line = reader.readLine();
					File file = new File(line);
					if (file.exists() || file.isFile()) {
						System.out
								.printf("===============>Load UBee firmware : %s\n"
										+ file.getAbsolutePath());
						this.zcs.loadUbeeFirmware(this, file.getName());
					} else {
						System.out.printf("File (%s) not found !!! \n",
								file.getAbsolutePath());
					}
				} else if (line.equals("b")) {
					Node zplug = this.searchNode("ZPLUG");
					if (zplug != null) {
						this.zcs.sendReadAttributesCommand(
								this.zclReader,
								zplug.getNwkAddr(),
								zplug.getEpByInCluster(ClusterName.Basic_Attributes
										.getId()),
								ClusterName.Basic_Attributes.getId(),
								new Attribute[] {
										Attribute.factory
												.newInstance(AttrConstants.ZCL_VERSION_ATTR),
										Attribute.factory
												.newInstance(AttrConstants.APP_VERSION_ATTR),
										Attribute.factory
												.newInstance(AttrConstants.STACK_VERSION_ATTR),
										Attribute.factory
												.newInstance(AttrConstants.HW_VERSION_ATTR),
										Attribute.factory
												.newInstance(AttrConstants.MANUFACTURER_NAME_ATTR),
										Attribute.factory
												.newInstance(AttrConstants.MODEL_IDENTIFIER_ATTR),
										Attribute.factory
												.newInstance(AttrConstants.DATE_CODE_ATTR),
										Attribute.factory
												.newInstance(AttrConstants.POWER_SOURCE_ATTR),
										Attribute.factory
												.newInstance(AttrConstants.LOCATION_DESCRIPTION_ATTR),
										Attribute.factory
												.newInstance(AttrConstants.PHYSICAL_ENVIRONMENT_ATTR),
										Attribute.factory
												.newInstance(AttrConstants.DEVICE_ENABLED_ATTR),
										Attribute.factory
												.newInstance(AttrConstants.ALARM_MASK_ATTR),
										Attribute.factory
												.newInstance(AttrConstants.DISABLE_LOCAL_CONFIG_ATTR) });
					} else {
						System.out.println("No ZPlug in network");
					}
				} else if (line.equals("c")) {
					Node zplug = this.searchNode("ZPLUG");
					if (zplug != null) {
						ReportingConfigurationAttribute attr = ReportingConfigurationAttribute.factory
								.newInstance(ReportingDirection.ValuesReported);
						attr.setIdentifier(AttrConstants.ON_OFF_ATTR);
						attr.setDataType(DataType.BOOLEAN);
						attr.setMinReportingInterval((short) 1);
						attr.setMaxReportingInterval((short) 0);
						this.zcs.sendConfigureReportingCommand(
								this.zclReportWriter, zplug.getNwkAddr(), zplug
										.getEpByInCluster(ClusterName.On_Off
												.getId()), ClusterName.On_Off
										.getId(),
								new ReportingConfigurationAttribute[] { attr });

						ReportingConfigurationAttribute attr1 = ReportingConfigurationAttribute.factory
								.newInstance(ReportingDirection.ValuesReported);
						attr1.setIdentifier(AttrConstants.CURRENT_SUMMATION_DELIVERED_ATTR);
						attr1.setDataType(DataType.UNSIGNED_48BITS);
						attr1.setMinReportingInterval((short) 60);
						attr1.setMaxReportingInterval((short) 3600);
						attr1.setReportableChange(100, attr1.getDataType());

						ReportingConfigurationAttribute attr2 = ReportingConfigurationAttribute.factory
								.newInstance(ReportingDirection.ValuesReported);
						attr2.setIdentifier(AttrConstants.INSTANTANEOUS_DEMAND_ATTR);
						attr2.setDataType(DataType.SIGNED_24BITS);
						attr2.setMinReportingInterval((short) 60);
						attr2.setMaxReportingInterval((short) 3600);
						attr2.setReportableChange(0x800000, attr2.getDataType());

						this.zcs.sendConfigureReportingCommand(
								this.zclReportWriter,
								zplug.getNwkAddr(),
								zplug.getEpByInCluster(ClusterName.Simple_Metering
										.getId()), ClusterName.Simple_Metering
										.getId(),
								new ReportingConfigurationAttribute[] { attr1,
										attr2 });
					} else {
						System.out.println("No ZPlug in network");
					}
					Node zrc = this.searchNode("ZRC");
					if (zrc != null) {
						ReportingConfigurationAttribute attr = ReportingConfigurationAttribute.factory
								.newInstance(ReportingDirection.ValuesReported);
						attr.setIdentifier(AttrConstants.MEASURED_VALUE_ATTR);
						attr.setDataType(DataType.SIGNED_16BITS);
						attr.setMinReportingInterval((short) 60);
						attr.setMaxReportingInterval((short) 180);
						this.zcs.sendConfigureReportingCommand(
								this.zclReportWriter,
								zrc.getNwkAddr(),
								zrc.getEpByInCluster(ClusterName.Temperature_Measurement
										.getId()),
								ClusterName.Temperature_Measurement.getId(),
								new ReportingConfigurationAttribute[] { attr });
					} else {
						System.out.println("No ZRC in network");
					}
				} else if (line.equals("d")) {
					Node zplug = this.searchNode("ZPLUG");
					if (zplug != null) {
						this.zcs.sendDiscoverAttributesCommand(
								this.zclDiscover,
								zplug.getNwkAddr(),
								zplug.getEpByInCluster(ClusterName.Basic_Attributes
										.getId()), ClusterName.Basic_Attributes
										.getId(), 0x0000, 30, true);
					} else {
						System.out.println("No ZPLUG in network\n");
					}
				} else if (line.equals("e")) {
					Node zplug = this.searchNode("ZPLUG");
					Node zrc = this.searchNode("ZRC");
					if (zplug != null && zrc != null) {
						this.zcs.sendBindRequest(this.zclBinder, zrc
								.getNwkAddr(), zrc.getIEEEAddress(), zrc
								.getEpByOutCluster(ClusterName.On_Off.getId()),
								ClusterName.On_Off.getId(), zplug
										.getIEEEAddress(), zplug
										.getEpByInCluster(ClusterName.On_Off
												.getId()));

					} else {
						System.out.println("No ZRC or ZPLUG in the network");
					}
				} else if (line.equals("f")) {
					Node zplug = this.searchNode("ZPLUG");
					Node zrc = this.searchNode("ZRC");
					if (zplug != null && zrc != null) {
						this.zcs.sendUnbindRequest(this.zclBinder, zrc
								.getNwkAddr(), zrc.getIEEEAddress(), zrc
								.getEpByOutCluster(ClusterName.On_Off.getId()),
								ClusterName.On_Off.getId(), zplug
										.getIEEEAddress(), zplug
										.getEpByInCluster(ClusterName.On_Off
												.getId()));

					} else {
						System.out.println("No ZRC or ZPLUG in the network");
					}
				} else if (line.equals("g")) {
					List<Integer> list = new ArrayList<Integer>();
					for (Node node : this.zcs.getNodes()) {
						list.add(Integer.valueOf(node.getNwkAddr()));
					}
					if (list.size() > 0) {
						int[] nwkAddrs = new int[list.size()];
						for (int i = 0; i < nwkAddrs.length; i++) {
							nwkAddrs[i] = list.get(i);
						}
						this.zcs.sendBindingsListRequest(this.zclBinder,
								nwkAddrs);
					}
				} else if (line.equals("i")) {
					Node[] nodes = this.zcs.getNodes();
					if (nodes.length > 0) {
						System.out.println("List of bindings of nodes");
						for (Node node : nodes) {
							for (Binding binding : node.getBindings()) {
								this.displayBinding(node, binding);
							}
						}
					} else {
						System.out.println("No node in the network");
					}
				} else if (line.equals("j")) {
					Node zplug = this.searchNode("ZPLUG");
					if (zplug != null) {
						int nwkAddr = zplug.getNwkAddr();
						// Search a node by its network address
						Node searchedNode = this.zars.searchNode(nwkAddr);
						if (searchedNode != null) {
							// Create a copy of the searched node
							Node node = (Node) searchedNode.clone();
							System.out.printf("Node 0x%04x '%s'\n",
									node.getNwkAddr(),
									node.getModelIdentifier());
						} else {
							System.out.println("ZPlug not found");
						}
					}
				} else if (line.equals("k")) {
					System.out.printf("Nodes count = %d\n",
							this.zcs.getNodes().length);
				} else if (line.equals("l")) {
					synchronized (this.mapOfNodes) {
						System.out.println();
						for (Node node : this.mapOfNodes.values()) {
							StringBuilder sb = new StringBuilder();
							for (Link link : node.getLinks()) {
								sb.append(String.format(" 0x%04x",
										link.getChildNwkAddr()));
							}
							System.out
									.printf("Node %s %s  parent = 0x%04x  children =%s\n",
											this.formatNodeName(node), node
													.getIEEEAddress()
													.toString(), node
													.getParentNwkAddr(), sb
													.toString());
						}
					}
				} else if (line.equals("m")) {
					for (Node node : this.zcs.getNodes()) {
						System.out.printf("Node 0x%04x '%s'\n",
								node.getNwkAddr(), node.getModelIdentifier());
						for (EndPoint ep : node.getEndPoints()) {
							System.out.printf("  EndPoint 0x%02x \n",
									ep.getNumber());
							for (Cluster inCl : ep.getInClusters()) {
								System.out.printf(
										"    In cluster %s(0x%04x) \n",
										inCl.getName(), inCl.getId());
							}
							for (Cluster outCl : ep.getOutClusters()) {
								System.out.printf(
										"    Out cluster %s(0x%04x) \n",
										outCl.getName(), outCl.getId());
							}
						}
					}
				} else if (line.equals("n")) {
					Node zrc = this.searchNode("ZRC");
					if (zrc != null) {
						// Subscribe for receiving the Zigbee notification
						try {
							this.zces
									.subscribe(this.observer, zrc.getNwkAddr());
							System.out
									.printf("===========> subscribe on ReportAttributes to "
											+ zrc.toString());
						} catch (Exception e) {
							System.err.println("Exception:" + e.getMessage());
						}
					} else {
						System.out.println("No ZRC in network");
					}
				} else if (line.equals("o")) {
					Node zplug = this.searchNode("ZPLUG");
					if (zplug != null) {
						try {
							this.zces.subscribe(this.observer,
									zplug.getNwkAddr());
							System.out
									.printf("===========> subscribe on ReportAttributes to "
											+ zplug.toString());
						} catch (Exception e) {
							System.err.println("Exception:" + e.getMessage());
						}
					} else {
						System.out.println("No ZPLUG in network");
					}
				} else if (line.equals("p")) {
					Node zplug = this.searchNode("ZPLUG");
					if (zplug != null) {
						ReadReportingConfigAttribute attr = ReadReportingConfigAttribute.factory
								.newInstance(ReportingDirection.ValuesReported);
						attr.setIdentifier(AttrConstants.ON_OFF_ATTR);
						this.zcs.sendReadReportingConfigurationCommand(
								this.zclReportReader, zplug.getNwkAddr(), zplug
										.getEpByInCluster(ClusterName.On_Off
												.getId()), ClusterName.On_Off
										.getId(),
								new ReadReportingConfigAttribute[] { attr });

						ReadReportingConfigAttribute attr2 = ReadReportingConfigAttribute.factory
								.newInstance(ReportingDirection.ValuesReported);
						attr2.setIdentifier(AttrConstants.CURRENT_SUMMATION_DELIVERED_ATTR);
						ReadReportingConfigAttribute attr1 = ReadReportingConfigAttribute.factory
								.newInstance(ReportingDirection.ValuesReported);
						attr1.setIdentifier(AttrConstants.INSTANTANEOUS_DEMAND_ATTR);
						this.zcs.sendReadReportingConfigurationCommand(
								this.zclReportReader,
								zplug.getNwkAddr(),
								zplug.getEpByInCluster(ClusterName.Simple_Metering
										.getId()), ClusterName.Simple_Metering
										.getId(),
								new ReadReportingConfigAttribute[] { attr2,
										attr1 });
					} else {
						System.out.println("No ZPlug in network");
					}
					Node zrc = this.searchNode("ZRC");
					if (zrc != null) {
						ReadReportingConfigAttribute attr = ReadReportingConfigAttribute.factory
								.newInstance(ReportingDirection.ValuesReported);
						attr.setIdentifier(AttrConstants.MEASURED_VALUE_ATTR);
						this.zcs.sendReadReportingConfigurationCommand(
								this.zclReportReader,
								zrc.getNwkAddr(),
								zrc.getEpByInCluster(ClusterName.Temperature_Measurement
										.getId()),
								ClusterName.Temperature_Measurement.getId(),
								new ReadReportingConfigAttribute[] { attr });
					} else {
						System.out.println("No ZRC in network");
					}
				} else if (line.equals("r")) {
					Node zplug = this.searchNode("ZPLUG");
					if (zplug != null) {
						this.zcs.sendReadAttributesCommand(
								this.zclReader,
								zplug.getNwkAddr(),
								zplug.getEpByInCluster(ClusterName.On_Off
										.getId()),
								ClusterName.On_Off.getId(),
								new Attribute[] { Attribute.factory
										.newInstance(AttrConstants.ON_OFF_ATTR) });
						this.zcs.sendReadAttributesCommand(
								this.zclReader,
								zplug.getNwkAddr(),
								zplug.getEpByInCluster(ClusterName.Basic_Attributes
										.getId()),
								ClusterName.Basic_Attributes.getId(),
								new Attribute[] { Attribute.factory
										.newInstance(AttrConstants.LOCATION_DESCRIPTION_ATTR) });
					} else {
						System.out.println("No ZPLUG in network");
					}
				} else if (line.equals("s")) {
					Node zplug = this.searchNode("ZPLUG");
					if (zplug != null) {
						this.zcs.sendClusterSpecificCommand(zplug.getNwkAddr(),
								zplug.getEpByInCluster(ClusterName.On_Off
										.getId()), ClusterName.On_Off.getId(),
								AttrConstants.TOGGLE_COMMAND, null);
					} else {
						System.out.println("No ZPLUG in network");
					}
				} else if (line.equals("t")) {
					debug = !debug;
					ZigbeeConnector.singleton.getZigbeeSettingsServices()
							.setDebugTrace(debug);
					System.out.printf("Debug trace : %s\n",
							Boolean.toString(debug));
				} else if (line.equals("u")) {
					Node zplug = this.searchNode("ZPLUG");
					if (zplug != null) {
						this.zcs.sendClusterSpecificCommand(
								this.zclClSpecCmdReader, zplug.getNwkAddr(),
								zplug.getEpByInCluster(ClusterName.Groups
										.getId()), ClusterName.Groups.getId(),
								AttrConstants.GET_GROUP_MEMBERSHIP_COMMAND,
								new byte[] { 0x00 });

						// Same request
						ClusterSpecCmdAttribute attr = ClusterSpecCmdAttribute.factory
								.newInstance();
						attr.setDataType(DataType.UNSIGNED_8BITS);
						attr.setDataAsNumber(0x00);
						this.zcs.sendClusterSpecificCommandExt(
								this.zclClSpecCmdReader, zplug.getNwkAddr(),
								zplug.getEpByInCluster(ClusterName.Groups
										.getId()), ClusterName.Groups.getId(),
								AttrConstants.GET_GROUP_MEMBERSHIP_COMMAND,
								new ClusterSpecCmdAttribute[] { attr });
					} else {
						System.out.println("No ZPLUG in network");
					}
				} else if (line.equals("v")) {
					System.out.println("ZCL library version : "
							+ zcs.getVersion());
				} else if (line.equals("w")) {
					Node zplug = this.searchNode("ZPLUG");
					if (zplug != null) {
						WriteAttribute attr = WriteAttribute.factory
								.newInstance();
						attr.setIdentifier(AttrConstants.LOCATION_DESCRIPTION_ATTR);
						attr.setDataType(DataType.CHARACTER_STRING);
						attr.setDataAsString(String.format("ZPLUG_%s",
								df.format(new Date())));
						this.zcs.sendWriteAttributesCommand(
								this.zclWriter,
								zplug.getNwkAddr(),
								zplug.getEpByInCluster(ClusterName.Basic_Attributes
										.getId()), ClusterName.Basic_Attributes
										.getId(), new WriteAttribute[] { attr });
					} else {
						System.out.println("No ZPLUG in network");
					}
				} else if (line.equals("x")) {
					// Stop UBEE
					System.out.printf("Stop UBEE %s \n",
							Boolean.toString(this.zcs.stopUbee()));
					// Start UBEE
					System.out.printf("Start UBEE %s \n",
							Boolean.toString(this.zcs.startUbee(this)));
				} else if (line.equals("y")) {
					// Force ZPlug to Leave the network
					Node zplug = this.searchNode("ZPLUG");
					if (zplug != null) {
						this.zcs.sendLeaveNetworkRequest(zplug.getNwkAddr());
					}
				} else if (line.equals("z")) {
					Node zplug = this.searchNode("ZPLUG");
					if (zplug != null) {
						ZCLFrame frame = ZCLFrame.factory.newZCLFrameInstance();
						frame.getHeader().getFrameControl()
								.setFrameType(FrameType.EntireProfile);
						frame.getHeader().getFrameControl()
								.setDirection(Direction.ClientToServer);
						frame.getHeader()
								.getFrameControl()
								.setManufacturerCode(
										ManufacturerCode.NotIncludeInZCLFrame);
						frame.getHeader()
								.getFrameControl()
								.setResponseStatus(
										ResponseStatus.ResponseReturned);
						ReadAttributesPayload payload = frame
								.createReadAttributesPayload();
						Attribute attr = Attribute.factory
								.newInstance(AttrConstants.MODEL_IDENTIFIER_ATTR);
						payload.addReadAttribute(attr);
						this.zcs.sendZCLFrameCommand(
								this.zclRespCallback,
								zplug.getNwkAddr(),
								zplug.getEpByInCluster(ClusterName.Basic_Attributes
										.getId()), ClusterName.Basic_Attributes
										.getId(), frame);

						frame = ZCLFrame.factory.newZCLFrameInstance();
						frame.getHeader().getFrameControl()
								.setFrameType(FrameType.SpecificCluster);
						frame.getHeader().getFrameControl()
								.setDirection(Direction.ClientToServer);
						frame.getHeader()
								.getFrameControl()
								.setManufacturerCode(
										ManufacturerCode.NotIncludeInZCLFrame);
						frame.getHeader()
								.getFrameControl()
								.setResponseStatus(
										ResponseStatus.ResponseReturned);
						frame.getHeader().setCmdIdentifierAsValue(
								AttrConstants.TOGGLE_COMMAND);
						this.zcs.sendZCLFrameCommand(this.zclRespCallback,
								zplug.getNwkAddr(), zplug
										.getEpByInCluster(ClusterName.On_Off
												.getId()), ClusterName.On_Off
										.getId(), frame);
					}
				} else if (line.equals("A")) {
					System.out.println("Authorized devices :");
					for (IEEEAddress device : this.zas
							.getAllAuthorizedDevices()) {
						System.out.println(" " + device.toSmallString());
					}
				} else if (line.equals("B")) {
					for (Node node : mapOfNodes.values()) {
						System.out.printf("Add authorized device %s : %s \n",
								node.getIEEEAddress(), Boolean
										.toString(this.zas
												.addAuthorizedDevice(node
														.getIEEEAddress())));
					}
				} else if (line.equals("C")) {
					System.out.printf("Clear authorized devices : %s \n",
							Boolean.toString(zas.clearAuthorizedDevices()));
				} else if (line.equals("D")) {
					Node zplug = this.searchNode("ZPLUG");
					if (zplug != null) {
						System.out.printf(
								"Remove authorized device %s : %s \n", zplug
										.getIEEEAddress(), Boolean
										.toString(this.zas
												.removeAuthorizedDevice(zplug
														.getIEEEAddress())));
					}
				} else if (line.equals("E")) {
					List<IEEEAddress> list = new ArrayList<IEEEAddress>();
					for (Node node : mapOfNodes.values()) {
						list.add(node.getIEEEAddress());
					}
					// Stop UBEE
					System.out.printf("Stop UBEE %s \n",
							Boolean.toString(this.zcs.stopUbee()));
					// Initialize the authorised devices
					this.zas.initAuthorizedDevices(list
							.toArray(new IEEEAddress[list.size()]));
					// Start UBEE
					System.out.printf("Start UBEE %s \n",
							Boolean.toString(this.zcs.startUbee(this)));
				} else if (line.equals("F")) {
					this.zcs.setNetworkChannel(20);
					this.zcs.setNetworkPanId(0x2222);
					this.zcs.resetUbee(true);
				} else if (line.equals("G")) {
					System.out.printf(
							"Network channel = %d - Pan ID = 0x%04x \n",
							this.zcs.getNetworkChannel(),
							this.zcs.getNetworkPanId());
				} else if (line.equals("O")) {
					int duration = 300;
					System.out.printf("Open association for %d sec.\n",
							duration);
					this.zcs.openAssociation(duration);
				}
			} catch (Exception e) {
				line = "h";
			}
		} while (!line.equals("q"));

		// Unsubscribe to receive notification events
		this.zces.unsubscribe(this, ZigbeeConnectorEventsSubscribe.allNodes);

		// Unsubscribe to receive node events
		this.zcs.unsubscribe(this);

		// Stop the communication with UBee
		System.out.println("StopUbee = "
				+ Boolean.toString(this.zcs.stopUbee()));

		System.exit(0);
	}

	@Override
	@SuppressWarnings("unused")
	public void notifyReportAttribute(int nwkAddr, int epNumber, int clusterId,
			int seqNumber, final ReportAttribute[] attributes) {
		try {
			this.displayReportAttribute("One", nwkAddr, epNumber, clusterId,
					attributes);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	@SuppressWarnings("unused")
	public void notifyClusterSpecificCommand(int nwkAddr, int epNumber,
			int clusterId, int seqNumber, byte command,
			ClusterSpecCmdAttribute[] attributes) {
		this.displayClusterSpecificCommand("One", nwkAddr, epNumber, clusterId,
				command, attributes);
	}

	private void displayReportAttribute(final String name, int nwkAddr,
			int epNumber, int clusterId, final ReportAttribute[] attributes)
			throws IOException {
		String[] tmp;

		for (ReportAttribute attr : attributes) {
			if (clusterId == ClusterName.On_Off.getId()) {
				System.out
						.printf("=====> Obs(%s) - notifyReportAttribute from 0x%04x Location(%s) Cl(0x%04x) : On/Off State = %s \n",
								name, nwkAddr, addressOfNodes.get(nwkAddr),
								clusterId,
								Boolean.toString(attr.getDataAsBoolean()));
			} else if (clusterId == ClusterName.Temperature_Measurement.getId()) {
				double degree = attr.getDataAsInteger() / 100;
				System.out
						.printf("=====> Obs(%s) - notifyReportAttribute from 0x%04x Location(%s) Cl(0x%04x) : Temperature = %.2f dC \n",
								name, nwkAddr, addressOfNodes.get(nwkAddr),
								clusterId, degree);
			} else if (clusterId == ClusterName.Occupancy_Sensing.getId()) {
				System.out
						.printf("=.....====> Obs(%s) - notifyReportAttribute from 0x%04x Location(%s) Cl(0x%04x) : OccupacyState = %s  \n",
								name, nwkAddr, addressOfNodes.get(nwkAddr),
								clusterId,
								Integer.toString(attr.getDataAsInteger()));

				tmp = new String[] { addressOfNodes.get(nwkAddr),
						"" + attr.getDataAsInteger(),
						timeCell.format(new Date()) };
				/*
				 * TODO detect presence of person
				 */
				// ZMovewriter.writeNext(tmp, false);
				// ZMovewriter.flush();

			} else if (clusterId == ClusterName.Illuminance_Measurement.getId()) {
				System.out
						.printf("=.....====> Obs(%s) - notifyReportAttribute from 0x%04x Location(%s) Cl(0x%04x) : illuminance = %d  \n",
								name, nwkAddr, addressOfNodes.get(nwkAddr),
								clusterId, attr.getDataAsInteger());

				tmp = new String[] { addressOfNodes.get(nwkAddr),
						"" + attr.getDataAsLong(), timeCell.format(new Date()) };

				/*
				 * TODO detect luminosity
				 */
				// ZLumwriter.writeNext(tmp, false);
				// ZLumwriter.flush();

			} else if (clusterId == ClusterName.Simple_Metering.getId()) {
				if (attr.getIdentifier() == AttrConstants.CURRENT_SUMMATION_DELIVERED_ATTR) {
					double kwh = attr.getDataAsLong() / 1000.0;
					System.out
							.printf("=====> Obs(%s) - notifyReportAttribute from 0x%04x Location(%s) Cl(0x%04x) : Consommation cumulee = %.2f kWh \n",
									name, nwkAddr, addressOfNodes.get(nwkAddr),
									clusterId, kwh);
				} else if (attr.getIdentifier() == AttrConstants.INSTANTANEOUS_DEMAND_ATTR) {
					System.out
							.printf("=====> Obs(%s) - notifyReportAttribute from 0x%04x Location(%s) Cl(0x%04x) : Consommation instantanee = %d W \n",
									name, nwkAddr, addressOfNodes.get(nwkAddr),
									clusterId, attr.getDataAsLong());// Récupérer
																		// la
																		// consommation
																		// de
																		// Zplug
					tmp = new String[] { addressOfNodes.get(nwkAddr),
							"" + attr.getDataAsLong(),
							timeCell.format(new Date()) };
					energy = attr.getDataAsLong();
				} else {
					System.out
							.printf("*********************=====> Obs(%s) - notifyReportAttribute from 0x%04x Location(%s) Cl(%s) \n",
									name, nwkAddr, addressOfNodes.get(nwkAddr),
									ClusterName.valueOf(clusterId));
				}
			} else {
				// System.out.printf("=====> Other  - notifyReportAttribute from 0x%04x Location(%d) Cl(%s) \n",
				// name, nwkAddr, epNumber, ClusterName.valueOf(clusterId));
				System.out.println("***************"
						+ ClusterName.valueOf(clusterId) + "    "
						+ addressOfNodes.get(nwkAddr));
			}
		}
	}

	private void displayClusterSpecificCommand(final String name, int nwkAddr,
			int epNumber, int clusterId, byte command,
			ClusterSpecCmdAttribute[] attributes) {
		try {
			StringBuilder sb = new StringBuilder(":");
			for (ClusterSpecCmdAttribute attr : attributes) {
				String str;
				if (attr.getDataType().isNumber()) {
					str = attr.getDataAsNumber().toString();
				} else if (attr.getDataType().isString()) {
					str = attr.getDataAsString();
				} else if (attr.getDataType().isBoolean()) {
					str = Boolean.toString(attr.getDataAsBoolean());
				} else {
					str = Arrays.toString(attr.getData());
				}
				sb.append(String.format("%s (%s)", str, attr.getDataType()
						.name()));
			}
			System.out
					.printf("=====> Obs(%s) - notifyClusterSpecificCommand from 0x%04x Location(%s) Cl(%s) - Command = 0x%02x %s \n",
							name, nwkAddr, addressOfNodes.get(nwkAddr),
							ClusterName.valueOf(clusterId), command,
							sb.toString());
			int i = (int) Integer.parseInt(attributes[0].getDataAsNumber()
					.toString());

			// i inform the state of the door or the presence of leak, if 1 the
			// door is open, if 0 the door is close
			if (i == 33)
				i = 1; // Door is open
			else if (i == 32)
				i = 0; // Door is close

			if (addressOfNodes.get(nwkAddr) != null) {
				String[] tmp = { addressOfNodes.get(nwkAddr), "" + i,
						timeCell.format(new Date()) };
				if (mapOfNodes.get(nwkAddr).getModelIdentifier()
						.equals("ZDOOR")) {

					/*
					 * TODO detect open/close door
					 */
					// ZDOORwriter.writeNext(tmp, false);
					// ZDOORwriter.flush();

				} else if (mapOfNodes.get(nwkAddr).getModelIdentifier()
						.equals("ZLEAK")) {

					/*
					 * TODO detect water
					 */
					// ZLeakwriter.writeNext(tmp, false);
					// ZLeakwriter.flush();
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void done(int percent) {
		System.out.printf("===============>Load UBee firmware : %d %% \n",
				percent);
	}

	private void displayBinding(final Node node, final Binding binding) {
		if (binding.isDstGroupAddr()) {
			System.out.printf(
					"Binding of %s 0x%04x : %s Ep(%d) Cl(%s) Group 0x%04x\n",
					node.getModelIdentifier(), node.getNwkAddr(), binding
							.getSrcIEEEAddr().toString(), binding
							.getSrcEpNumber(), ClusterName.valueOf(binding
							.getClusterId()), binding.getDstGroupAddr());
		} else {
			Node destNode = searchNode(binding.getDstIEEEAddr());
			System.out
					.printf("Binding of %s 0x%04x : %s Ep(%d) Cl(%s) ==> %s %s Ep(%d)\n",
							node.getModelIdentifier(), node.getNwkAddr(),
							binding.getSrcIEEEAddr().toString(), binding
									.getSrcEpNumber(), ClusterName
									.valueOf(binding.getClusterId()),
							(destNode != null ? destNode.getModelIdentifier()
									: "Unknown"), binding.getDstIEEEAddr()
									.toString(), binding.getDstEpNumber());
		}
	}

	@Override
	public void ubeeStarted() {
		System.err.println("====> ubeeStarted notification");
		System.err.printf("UBee License validity for %s : %s",
				this.zcs.getCoordinator(),
				Boolean.toString(this.zcs.isLicenseValid()));
		date = new Date();
		String fileName = ft.format(date).toString() + ".csv";
	}

	@Override
	public void ubeeReleased() {
		System.err.println("====> ubeeReleased notification");
		this.mapOfNodes.clear();
	}

	@Override
	public void ubeeStopped() {
		System.err.println("====> ubeeStopped notification");
	}

	@Override
	public void ubeeWaitingBoot() {
		System.err.println("====> ubeeWaitingBoot notification");
	}

	@Override
	public void notifyNodeCreated(final Node node, final Reason reason) {
		System.out.printf("====> notifyNodeCreated %s - Reason(%s)\n",
				this.formatNodeName(node), reason.name());
		this.mapOfNodes.put(node.getNwkAddr(), node);
		sendReadLocationDescription(node);
	}

	@Override
	public void notifyNodeUpdated(final Node node, final Reason reason) {
		System.out.printf("====> notifyNodeUpdated %s - Reason(%s)\n",
				this.formatNodeName(node), reason.name());
		this.mapOfNodes.put(node.getNwkAddr(), node);

		if (reason == Reason.Bindings) {
			if ("ZPLUG".equals(node.getModelIdentifier())) {
				createBindingsForZPLug(node);
			} else if ("ZRC".equals(node.getModelIdentifier())) {
				createBindingsForZRC(node);
			} else if ("ZDOOR".equals(node.getModelIdentifier())) {
				createBindingsForZDoor(node);
			} else if ("ZMOVE".equals(node.getModelIdentifier())) {
				createBindingsForZMove(node);
			} else if ("ZLEAK".equals(node.getModelIdentifier())) {
				createBindingsForZLeak(node);
			} else if ("ZLUM".equals(node.getModelIdentifier())) {
				createBindingsForZLum(node);
			}
		}
	}

	@Override
	public void notifyNodeDeleted(final Node node, final Reason reason) {
		System.out.printf("====> notifyNodeDeleted %s - Reason(%s)\n",
				this.formatNodeName(node), reason.name());
		this.mapOfNodes.remove(node.getNwkAddr());
	}

	private boolean isBindingAlreadyCreated(final Node srcNode,
			final ClusterName cluster, final Node dstNode) {
		for (Binding binding : srcNode.getBindings()) {
			if (binding.getClusterId() == cluster.getId()
					&& binding.isDstGroupAddr() != true
					&& srcNode.getIEEEAddress()
							.equals(binding.getSrcIEEEAddr())
					&& dstNode.getIEEEAddress()
							.equals(binding.getDstIEEEAddr())
					&& srcNode.getEpByInCluster(cluster.getId()) == binding
							.getSrcEpNumber()
					&& dstNode.getEpByOutCluster(cluster.getId()) == binding
							.getDstEpNumber()) {
				return true;
			}
		}
		return false;
	}

	private void createBindingsForZPLug(final Node node) {
		Node ubee = searchNode("UBEE");
		if (ubee != null) {
			// Check if the binding is already created
			if (isBindingAlreadyCreated(node, ClusterName.On_Off, ubee) != true) {
				this.zcs.sendBindRequest(this.zclBinder, node.getNwkAddr(),
						node.getIEEEAddress(),
						node.getEpByInCluster(ClusterName.On_Off.getId()),
						ClusterName.On_Off.getId(), ubee.getIEEEAddress(),
						ubee.getEpByOutCluster(ClusterName.On_Off.getId()));
				System.out
						.printf("===========> Create Binding On/Off between %s --> UBEE \n",
								node.toString());
			}

			// Check if the binding is already created
			if (isBindingAlreadyCreated(node, ClusterName.Simple_Metering, ubee) != true) {
				this.zcs.sendBindRequest(this.zclBinder, node.getNwkAddr(),
						node.getIEEEAddress(), node
								.getEpByInCluster(ClusterName.Simple_Metering
										.getId()), ClusterName.Simple_Metering
								.getId(), ubee.getIEEEAddress(), ubee
								.getEpByOutCluster(ClusterName.Simple_Metering
										.getId()));
				System.out
						.printf("===========> Create Binding Simple Metering between %s --> UBEE \n",
								node.toString());
			}
		}
	}

	private void createBindingsForZRC(final Node node) {
		Node ubee = searchNode("UBEE");
		if (ubee != null) {
			// Check if the binding is already created
			if (isBindingAlreadyCreated(node,
					ClusterName.Temperature_Measurement, ubee) != true) {
				this.zcs.sendBindRequest(
						this.zclBinder,
						node.getNwkAddr(),
						node.getIEEEAddress(),
						node.getEpByInCluster(ClusterName.Temperature_Measurement
								.getId()),
						ClusterName.Temperature_Measurement.getId(),
						ubee.getIEEEAddress(),
						ubee.getEpByOutCluster(ClusterName.Temperature_Measurement
								.getId()));
				System.out
						.printf("===========> Create Binding Temperature Measurement between %s --> UBEE \n",
								node.toString());
			}

			if (isBindingAlreadyCreated(node, ClusterName.Alarms, ubee) != true) {
				this.zcs.sendBindRequest(this.zclBinder, node.getNwkAddr(),
						node.getIEEEAddress(),
						node.getEpByInCluster(ClusterName.Alarms.getId()),
						ClusterName.Alarms.getId(), ubee.getIEEEAddress(),
						ubee.getEpByOutCluster(ClusterName.Alarms.getId()));
				System.out
						.printf("===========> Create Binding Alarms between ZRC %s --> UBEE \n",
								node.toString());
			}
		}
	}

	private void createBindingsForZDoor(final Node node) {
		Node ubee = searchNode("UBEE");
		if (ubee != null) {
			// Check if the binding is already created
			if (isBindingAlreadyCreated(node, ClusterName.IAS_Zone, ubee) != true) {
				this.zcs.sendBindRequest(this.zclBinder, node.getNwkAddr(),
						node.getIEEEAddress(),
						node.getEpByInCluster(ClusterName.IAS_Zone.getId()),
						ClusterName.IAS_Zone.getId(), ubee.getIEEEAddress(),
						ubee.getEpByOutCluster(ClusterName.IAS_Zone.getId()));
				System.out
						.printf("===========> Create Binding ZDoor Measurement between %s --> UBEE \n",
								node.toString());
			}

			if (isBindingAlreadyCreated(node, ClusterName.Alarms, ubee) != true) {
				this.zcs.sendBindRequest(this.zclBinder, node.getNwkAddr(),
						node.getIEEEAddress(),
						node.getEpByInCluster(ClusterName.Alarms.getId()),
						ClusterName.Alarms.getId(), ubee.getIEEEAddress(),
						ubee.getEpByOutCluster(ClusterName.Alarms.getId()));
				System.out
						.printf("===========> Create Binding Alarms between ZDoor %s --> UBEE \n",
								node.toString());
			}
		}
	}

	private void createBindingsForZMove(final Node node) {
		Node ubee = searchNode("UBEE");
		if (ubee != null) {
			// Check if the binding is already created
			if (isBindingAlreadyCreated(node, ClusterName.Occupancy_Sensing,
					ubee) != true) {
				this.zcs.sendBindRequest(this.zclBinder, node.getNwkAddr(),
						node.getIEEEAddress(), node
								.getEpByInCluster(ClusterName.Occupancy_Sensing
										.getId()),
						ClusterName.Occupancy_Sensing.getId(), ubee
								.getIEEEAddress(),
						ubee.getEpByOutCluster(ClusterName.Occupancy_Sensing
								.getId()));
				System.out
						.printf("===========> Create Binding ZMove Measurement between %s --> UBEE \n",
								node.toString());
			}

			if (isBindingAlreadyCreated(node, ClusterName.Alarms, ubee) != true) {
				this.zcs.sendBindRequest(this.zclBinder, node.getNwkAddr(),
						node.getIEEEAddress(),
						node.getEpByInCluster(ClusterName.Alarms.getId()),
						ClusterName.Alarms.getId(), ubee.getIEEEAddress(),
						ubee.getEpByOutCluster(ClusterName.Alarms.getId()));
				System.out
						.printf("===========> Create Binding Alarms between Zmove %s --> UBEE \n",
								node.toString());
			}
		}
	}

	private void createBindingsForZLeak(final Node node) {
		Node ubee = searchNode("UBEE");
		if (ubee != null) {
			// Check if the binding is already created
			if (isBindingAlreadyCreated(node, ClusterName.IAS_Zone, ubee) != true) {
				this.zcs.sendBindRequest(this.zclBinder, node.getNwkAddr(),
						node.getIEEEAddress(),
						node.getEpByInCluster(ClusterName.IAS_Zone.getId()),
						ClusterName.IAS_Zone.getId(), ubee.getIEEEAddress(),
						ubee.getEpByOutCluster(ClusterName.IAS_Zone.getId()));
				System.out
						.printf("===========> Create Binding Zleak Measurement between %s --> UBEE \n",
								node.toString());
			}

			if (isBindingAlreadyCreated(node, ClusterName.Alarms, ubee) != true) {
				this.zcs.sendBindRequest(this.zclBinder, node.getNwkAddr(),
						node.getIEEEAddress(),
						node.getEpByInCluster(ClusterName.Alarms.getId()),
						ClusterName.Alarms.getId(), ubee.getIEEEAddress(),
						ubee.getEpByOutCluster(ClusterName.Alarms.getId()));
				System.out
						.printf("===========> Create Binding Alarms between Zleak %s --> UBEE \n",
								node.toString());
			}
		}
	}

	private void createBindingsForZLum(final Node node) {
		Node ubee = searchNode("UBEE");
		if (ubee != null) {
			// Check if the binding is already created
			if (isBindingAlreadyCreated(node,
					ClusterName.Illuminance_Measurement, ubee) != true) {
				this.zcs.sendBindRequest(
						this.zclBinder,
						node.getNwkAddr(),
						node.getIEEEAddress(),
						node.getEpByInCluster(ClusterName.Illuminance_Measurement
								.getId()),
						ClusterName.Illuminance_Measurement.getId(),
						ubee.getIEEEAddress(),
						ubee.getEpByOutCluster(ClusterName.Illuminance_Measurement
								.getId()));
				System.out
						.printf("===========> Create Binding Zlum Measurement between %s --> UBEE \n",
								node.toString());
			}

			if (isBindingAlreadyCreated(node, ClusterName.Alarms, ubee) != true) {
				this.zcs.sendBindRequest(this.zclBinder, node.getNwkAddr(),
						node.getIEEEAddress(),
						node.getEpByInCluster(ClusterName.Alarms.getId()),
						ClusterName.Alarms.getId(), ubee.getIEEEAddress(),
						ubee.getEpByOutCluster(ClusterName.Alarms.getId()));
				System.out
						.printf("===========> Create Binding Alarms between Zlum %s --> UBEE \n",
								node.toString());
			}
		}
	}

	protected void sendReadLocationDescription(final Node node) {
		Attribute attr = Attribute.factory
				.newInstance(AttrConstants.LOCATION_DESCRIPTION_ATTR);
		int epNumber = node.getEpByInCluster(ClusterName.Basic_Attributes
				.getId());
		this.zcs.sendReadAttributesCommand(this.zclReader, node.getNwkAddr(),
				epNumber, ClusterName.Basic_Attributes.getId(),
				new Attribute[] { attr });
	}

	private String formatNodeName(final Node node) {
		String modelId = node.getModelIdentifier();
		modelId = (modelId == null || modelId.isEmpty()) ? "Unknown" : modelId;
		return String.format("0x%04X %s", node.getNwkAddr(), modelId);
	}
//
//	public static void main(String[] args) {
//		new TestZCL();
//	}

	public static long getEnergy() {

		return energy;
	}
}
