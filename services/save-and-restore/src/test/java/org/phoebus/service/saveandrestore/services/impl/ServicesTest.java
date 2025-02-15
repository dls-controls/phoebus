/**
 * Copyright (C) 2018 European Spallation Source ERIC.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.phoebus.service.saveandrestore.services.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.phoebus.service.saveandrestore.services.IServices;
import org.phoebus.service.saveandrestore.services.config.ServicesTestConfig;
import org.phoebus.service.saveandrestore.services.exception.NodeNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import org.phoebus.applications.saveandrestore.model.ConfigPv;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.service.saveandrestore.persistence.dao.NodeDAO;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextHierarchy({ @ContextConfiguration(classes = { ServicesTestConfig.class}) })
public class ServicesTest {

	@Autowired
	private IServices services;

	@Autowired
	private NodeDAO nodeDAO;

	private Node configFromClient;

	private Node config1;

	private Node configWithParent;

	List<ConfigPv> configPvList;


	@Before
	public void setUp(){

		ConfigPv configPv = ConfigPv.builder()
				.pvName("pvName")
				.build();

		configFromClient = Node.builder()
				.nodeType(NodeType.CONFIGURATION)
				.build();

		configFromClient.setId(1);
		configFromClient.setCreated(new Date());


		config1 = Node.builder()
				.nodeType(NodeType.CONFIGURATION)
				.build();

		config1.setId(1);

		configWithParent = Node.builder()
				.nodeType(NodeType.CONFIGURATION)
				.build();

		configPvList = Arrays.asList(configPv);
	}

	@Test
	public void testCreateConfiguration() {
		when(nodeDAO.getNode("a")).thenReturn(Node.builder().id(1).uniqueId("a").build());
		services.createNode("a", configWithParent);
	}

	@Test
	public void testGetConfigNotNull() {

		when(nodeDAO.getNode("a")).thenReturn(configFromClient);
		Node config = services.getNode("a");
		assertEquals(1, config.getId());
	}

	@Test
	public void testGetSnapshots() {

		services.getSnapshots(anyString());
		verify(nodeDAO, times(1)).getSnapshots(anyString());
		reset(nodeDAO);
	}

	@Test
	public void testGetSnapshot() {

		when(nodeDAO.getSnapshot("s")).thenReturn(mock(Node.class));
		Node snapshot = services.getSnapshot("s");
		assertNotNull(snapshot);
		reset(nodeDAO);
	}

	@Test
	public void testCreateNewFolder() {

		Node folderFromClient = Node.builder().name("SomeFolder").build();

		when(nodeDAO.getNode("p")).thenReturn(Node.builder().build());
		services.createNode("p", folderFromClient);
		reset(nodeDAO);
	}

	@Test
	public void testCreateSnapshot() {

		Node folderFromClient = Node.builder().name("snapshot").nodeType(NodeType.SNAPSHOT).build();
		Node parent = new Node();
		parent.setId(7);
		parent.setNodeType(NodeType.CONFIGURATION);
		when(nodeDAO.getNode(parent.getUniqueId())).thenReturn(parent);
		services.createNode(parent.getUniqueId(), folderFromClient);
		reset(nodeDAO);
	}

	@Test
	public void testGetFolder() {

		when(nodeDAO.getNode("a")).thenReturn(Node.builder().id(77).uniqueId("a").build());
		assertNotNull(services.getNode("a"));
	}


	@Test
	public void testGetNonExsitingFolder() {

		when(nodeDAO.getNode("a")).thenReturn(null);
		assertNull(services.getNode("a"));

		reset(nodeDAO);
	}

	@Test
	public void testDeleteNode() {
		Node node = new Node();
		node.setId(7);
		when(nodeDAO.getNode("a")).thenReturn(node);
		services.deleteNode("a");

		verify(nodeDAO, atLeast(1)).deleteNode("a");
		reset(nodeDAO);
	}

	@Test
	public void testUpdateNode() {
		Node node = Node.builder().id(7).name("name").build();
		Node parent = new Node();
		when(nodeDAO.getNode(node.getUniqueId())).thenReturn(node);
		when(nodeDAO.getParentNode(node.getUniqueId())).thenReturn(parent);
		when(nodeDAO.getChildNodes(parent.getUniqueId())).thenReturn(Collections.emptyList());
		services.updateNode(node);
		verify(nodeDAO, atLeast(1)).updateNode(node, false);
		reset(nodeDAO);
	}

	@Test
	public void testUpdateNodeNoNameAndTypeClash() {
		Node node = Node.builder().id(7).name("name").nodeType(NodeType.FOLDER).build();
		Node parent = new Node();
		when(nodeDAO.getNode(node.getUniqueId())).thenReturn(node);
		when(nodeDAO.getParentNode(node.getUniqueId())).thenReturn(parent);
		Node childNode = Node.builder().id(77).name("name").nodeType(NodeType.CONFIGURATION).build();
		when(nodeDAO.getChildNodes(parent.getUniqueId())).thenReturn(Arrays.asList(childNode));
		services.updateNode(node);
		verify(nodeDAO, atLeast(1)).updateNode(node, false);
		reset(nodeDAO);
	}

	@Test
	public void testUpdateNodeParentNotFound() {
		Node node = Node.builder().id(7).name("name").build();
		Node parent = new Node();
		when(nodeDAO.getNode(node.getUniqueId())).thenReturn(node);
		when(nodeDAO.getParentNode(node.getUniqueId())).thenReturn(parent);
		when(nodeDAO.getChildNodes(parent.getUniqueId())).thenReturn(Collections.emptyList());
		services.updateNode(node);
		verify(nodeDAO, atLeast(1)).updateNode(node, false);
		reset(nodeDAO);
	}

	@Test
	public void testTimestamp() {
		long timeNanoSeconds = 1538037556314456383L;

		System.out.println(new Date(timeNanoSeconds / 1000000));
	}

	@Test
	public void testUpdateConfiguration() {

		when(nodeDAO.updateConfiguration(config1, configPvList)).thenReturn(config1);

		assertNotNull(services.updateConfiguration(config1, configPvList));
	}

	@Test
	public void testGetSnapshotItems() {
		when(nodeDAO.getSnapshotItems("a")).thenReturn(Collections.emptyList());

		assertNotNull(services.getSnapshotItems("a"));
	}

	@Test
	public void testgetParentNode() {
		Node parentNode = Node.builder().name("a").uniqueId("u").build();
		when(nodeDAO.getParentNode("u")).thenReturn(parentNode);

		assertNotNull(services.getParentNode("u"));
	}

	@Test
	public void testGetChildNodes() {
		when(nodeDAO.getChildNodes("a")).thenReturn(Arrays.asList(Node.builder().build()));
		assertNotNull(services.getChildNodes("a"));
	}

	@Test
	public void testGetRootNode() {
		when(nodeDAO.getRootNode()).thenReturn(Node.builder().build());
		assertNotNull(services.getRootNode());
	}

	@Test
	public void testGetConfigPvs() {
		when(nodeDAO.getConfigPvs("a")).thenReturn(Arrays.asList(ConfigPv.builder().build()));
		assertNotNull(services.getConfigPvs("a"));
	}

	@Test
	public void testSaveSnapshot() {
		when(nodeDAO.saveSnapshot("a", Collections.emptyList(), "b", "c", "d")).thenReturn(Node.builder().nodeType(NodeType.SNAPSHOT).build());
		assertNotNull(services.saveSnapshot("a", Collections.emptyList(), "b", "d", "c"));
	}

	@Test
	public void testGetFromPath(){
		Node node = Node.builder().name("SomeFolder").build();
		when(nodeDAO.getFromPath("path")).thenReturn(Arrays.asList(node));
		assertEquals("SomeFolder", services.getFromPath("path").get(0).getName());
	}

	@Test
	public void testGetFullPath(){
		when(nodeDAO.getFullPath("nodeId")).thenReturn("/a/b/c");
		assertEquals("/a/b/c", nodeDAO.getFullPath("nodeId"));
	}

	@Test
	public void testMoveNode(){
		Node nodeToMove = new Node();
		nodeToMove.setId(7);
		Node targetNode = new Node();

		when(nodeDAO.getNode(nodeToMove.getUniqueId())).thenReturn(nodeToMove);
		when(nodeDAO.getNode(targetNode.getUniqueId())).thenReturn(targetNode);
		when(nodeDAO.getChildNodes(targetNode.getUniqueId())).thenReturn(Collections.emptyList());

		services.moveNodes(Arrays.asList(nodeToMove.getUniqueId()), targetNode.getUniqueId(), "user");
		reset(nodeDAO);
	}

	@Test
	public void testMoveNodeTargetContainsDifferentType(){
		Node nodeToMove = new Node();
		nodeToMove.setId(7);
		nodeToMove.setName("name");
		nodeToMove.setNodeType(NodeType.CONFIGURATION);

		Node targetNode = new Node();

		Node childNode = new Node();
		childNode.setId(77);
		childNode.setName("name");
		childNode.setNodeType(NodeType.FOLDER);

		when(nodeDAO.getNode(nodeToMove.getUniqueId())).thenReturn(nodeToMove);
		when(nodeDAO.getNode(targetNode.getUniqueId())).thenReturn(targetNode);
		when(nodeDAO.getChildNodes(targetNode.getUniqueId())).thenReturn(Arrays.asList(childNode));

		services.moveNodes(Arrays.asList(nodeToMove.getUniqueId()), targetNode.getUniqueId(), "user");
		reset(nodeDAO);
	}
}
