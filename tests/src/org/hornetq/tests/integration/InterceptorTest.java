/*
 * Copyright 2009 Red Hat, Inc.
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package org.hornetq.tests.integration;

import org.hornetq.core.client.ClientConsumer;
import org.hornetq.core.client.ClientMessage;
import org.hornetq.core.client.ClientProducer;
import org.hornetq.core.client.ClientSession;
import org.hornetq.core.client.ClientSessionFactory;
import org.hornetq.core.exception.HornetQException;
import org.hornetq.core.logging.Logger;
import org.hornetq.core.message.Message;
import org.hornetq.core.remoting.Interceptor;
import org.hornetq.core.remoting.Packet;
import org.hornetq.core.remoting.RemotingConnection;
import org.hornetq.core.remoting.impl.wireformat.PacketImpl;
import org.hornetq.core.remoting.impl.wireformat.SessionReceiveMessage;
import org.hornetq.core.remoting.impl.wireformat.SessionSendMessage;
import org.hornetq.core.server.HornetQServer;
import org.hornetq.core.server.ServerMessage;
import org.hornetq.tests.util.ServiceTestBase;
import org.hornetq.utils.SimpleString;

/**
 * 
 * A InterceptorTest
 *
 * @author tim fox
 *
 *
 */
public class InterceptorTest extends ServiceTestBase
{
   private static final Logger log = Logger.getLogger(InterceptorTest.class);

   private HornetQServer server;

   private final SimpleString QUEUE = new SimpleString("InterceptorTestQueue");

   @Override
   protected void setUp() throws Exception
   {
      super.setUp();

      server = createServer(false);

      server.start();
   }

   @Override
   protected void tearDown() throws Exception
   {
      server.stop();

      server = null;

      super.tearDown();
   }
   
   private static final String key = "fruit";
   
   private class MyInterceptor1 implements Interceptor
   {
      public boolean intercept(Packet packet, RemotingConnection connection) throws HornetQException
      {
         if (packet.getType() == PacketImpl.SESS_SEND)
         {
            SessionSendMessage p = (SessionSendMessage)packet;
            
            ServerMessage sm = p.getServerMessage();
            
            sm.putStringProperty(key, "orange");
         }
         
         return true;
      }
      
   }
   
   private class MyInterceptor2 implements Interceptor
   {
      public boolean intercept(Packet packet, RemotingConnection connection) throws HornetQException
      {
         if (packet.getType() == PacketImpl.SESS_SEND)
         {
            return false;
         }
         
         return true;
      }
      
   }
   
   private class MyInterceptor3 implements Interceptor
   {
      public boolean intercept(Packet packet, RemotingConnection connection) throws HornetQException
      {
         if (packet.getType() == PacketImpl.SESS_RECEIVE_MSG)
         {
            SessionReceiveMessage p = (SessionReceiveMessage)packet;
            
            ClientMessage cm = p.getClientMessage();
            
            cm.putStringProperty(key, "orange");
         }
         
         return true;
      }
      
   }
   
   private class MyInterceptor4 implements Interceptor
   {
      public boolean intercept(Packet packet, RemotingConnection connection) throws HornetQException
      {
         if (packet.getType() == PacketImpl.SESS_RECEIVE_MSG)
         {
            return false;
         }
         
         return true;
      }
      
   }
   
   private class MyInterceptor5 implements Interceptor
   {
      private final String key;
      
      private final int num;
      
      private volatile boolean reject;
      
      private volatile boolean wasCalled;
      
      MyInterceptor5(final String key, final int num)
      {
         this.key = key;
         
         this.num = num;
      }
      
      public void setReject(final boolean reject)
      {
         this.reject = reject;
      }
      
      public boolean wasCalled()
      {
         return wasCalled;
      }
      
      public void setWasCalled(final boolean wasCalled)
      {
         this.wasCalled = wasCalled;
      }
      
      public boolean intercept(Packet packet, RemotingConnection connection) throws HornetQException
      {
         if (packet.getType() == PacketImpl.SESS_SEND)
         {
            SessionSendMessage p = (SessionSendMessage)packet;
            
            ServerMessage sm = p.getServerMessage();
            
            sm.putIntProperty(key, num);
            
            wasCalled = true;
            
            return !reject;
         }
         
         return true;
         
      }
      
   }
   
   private class MyInterceptor6 implements Interceptor
   {
      private final String key;
      
      private final int num;
      
      private volatile boolean reject;
      
      private volatile boolean wasCalled;
      
      MyInterceptor6(final String key, final int num)
      {
         this.key = key;
         
         this.num = num;
      }
      
      public void setReject(final boolean reject)
      {
         this.reject = reject;
      }
      
      public boolean wasCalled()
      {
         return wasCalled;
      }
      
      public void setWasCalled(final boolean wasCalled)
      {
         this.wasCalled = wasCalled;
      }
      
      public boolean intercept(Packet packet, RemotingConnection connection) throws HornetQException
      {
         if (packet.getType() == PacketImpl.SESS_RECEIVE_MSG)
         {
            SessionReceiveMessage p = (SessionReceiveMessage)packet;
            
            Message sm = p.getClientMessage();
            
            sm.putIntProperty(key, num);
            
            wasCalled = true;
            
            return !reject;
         }
         
         return true;
         
      }
      
   }
   
   public void testServerInterceptorChangeProperty() throws Exception
   {
      MyInterceptor1 interceptor = new MyInterceptor1();
      
      server.getRemotingService().addInterceptor(interceptor);
      
      ClientSessionFactory sf = createInVMFactory();

      ClientSession session = sf.createSession(false, true, true, true);

      session.createQueue(QUEUE, QUEUE, null, false);

      ClientProducer producer = session.createProducer(QUEUE);

      final int numMessages = 10;
            
      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message = session.createClientMessage(false);
         
         message.putStringProperty(key, "apple");
         
         producer.send(message);
      }

      ClientConsumer consumer = session.createConsumer(QUEUE);
      
      session.start();
      
      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message = consumer.receive(1000);
         
         assertEquals("orange", message.getStringProperty(key));
      }
      
      server.getRemotingService().removeInterceptor(interceptor);
      
      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message = session.createClientMessage(false);
         
         message.putStringProperty(key, "apple");
         
         producer.send(message);
      }

      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message = consumer.receive(1000);
         
         assertEquals("apple", message.getStringProperty(key));
      }
     
      session.close();
   }
   
   public void testServerInterceptorRejectPacket() throws Exception
   {
      MyInterceptor2 interceptor = new MyInterceptor2();
      
      server.getRemotingService().addInterceptor(interceptor);
      
      ClientSessionFactory sf = createInVMFactory();
      
      sf.setBlockOnNonPersistentSend(false);

      ClientSession session = sf.createSession(false, true, true, true);

      session.createQueue(QUEUE, QUEUE, null, false);

      ClientProducer producer = session.createProducer(QUEUE);

      final int numMessages = 10;
            
      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message = session.createClientMessage(false);
         
         producer.send(message);
      }

      ClientConsumer consumer = session.createConsumer(QUEUE);
      
      session.start();
      
      ClientMessage message = consumer.receiveImmediate();

      assertNull(message);
     
      session.close();
   }
   
   public void testClientInterceptorChangeProperty() throws Exception
   {          
      ClientSessionFactory sf = createInVMFactory();
      
      MyInterceptor3 interceptor = new MyInterceptor3();
      
      sf.addInterceptor(interceptor);
 
      ClientSession session = sf.createSession(false, true, true, true);

      session.createQueue(QUEUE, QUEUE, null, false);

      ClientProducer producer = session.createProducer(QUEUE);

      final int numMessages = 10;
            
      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message = session.createClientMessage(false);
         
         message.putStringProperty(key, "apple");
         
         producer.send(message);
      }

      ClientConsumer consumer = session.createConsumer(QUEUE);
      
      session.start();
      
      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message = consumer.receive(1000);
         
         assertEquals("orange", message.getStringProperty(key));
      }
      
      sf.removeInterceptor(interceptor);
      
      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message = session.createClientMessage(false);
         
         message.putStringProperty(key, "apple");
         
         producer.send(message);
      }

      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message = consumer.receive(1000);
         
         assertEquals("apple", message.getStringProperty(key));
      }
     
      session.close();
   }
   
   public void testClientInterceptorRejectPacket() throws Exception
   {          
      ClientSessionFactory sf = createInVMFactory();
      
      MyInterceptor4 interceptor = new MyInterceptor4();
      
      sf.addInterceptor(interceptor);
      
      ClientSession session = sf.createSession(false, true, true, true);

      session.createQueue(QUEUE, QUEUE, null, false);

      ClientProducer producer = session.createProducer(QUEUE);

      final int numMessages = 10;
            
      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message = session.createClientMessage(false);
         
         producer.send(message);
      }

      ClientConsumer consumer = session.createConsumer(QUEUE);
      
      session.start();
      
      ClientMessage message = consumer.receive(100);

      assertNull(message);
     
      session.close();
   }
   
   public void testServerMultipleInterceptors() throws Exception
   {
      MyInterceptor5 interceptor1 = new MyInterceptor5("a", 1);
      MyInterceptor5 interceptor2 = new MyInterceptor5("b", 2);
      MyInterceptor5 interceptor3 = new MyInterceptor5("c", 3);
      MyInterceptor5 interceptor4 = new MyInterceptor5("d", 4);
      
      server.getRemotingService().addInterceptor(interceptor1);
      server.getRemotingService().addInterceptor(interceptor2);
      server.getRemotingService().addInterceptor(interceptor3);
      server.getRemotingService().addInterceptor(interceptor4);
      
      ClientSessionFactory sf = createInVMFactory();

      ClientSession session = sf.createSession(false, true, true, true);

      session.createQueue(QUEUE, QUEUE, null, false);

      ClientProducer producer = session.createProducer(QUEUE);

      final int numMessages = 10;
            
      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message = session.createClientMessage(false);
         
         producer.send(message);
      }

      ClientConsumer consumer = session.createConsumer(QUEUE);
      
      session.start();
      
      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message = consumer.receive(1000);
         
         assertEquals(1, message.getIntProperty("a").intValue());
         assertEquals(2, message.getIntProperty("b").intValue());
         assertEquals(3, message.getIntProperty("c").intValue());
         assertEquals(4, message.getIntProperty("d").intValue());
      }
      
      server.getRemotingService().removeInterceptor(interceptor2);
      
      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message = session.createClientMessage(false);
         
         producer.send(message);
      }

      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message = consumer.receive(1000);
         
         assertEquals(1, message.getIntProperty("a").intValue());
         assertFalse(message.containsProperty("b"));
         assertEquals(3, message.getIntProperty("c").intValue());
         assertEquals(4, message.getIntProperty("d").intValue());
        
      }
      
      interceptor3.setReject(true);
      
      interceptor1.setWasCalled(false);
      interceptor2.setWasCalled(false);
      interceptor3.setWasCalled(false);
      interceptor4.setWasCalled(false);
      
      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message = session.createClientMessage(false);
         
         producer.send(message);
      }

      ClientMessage message = consumer.receiveImmediate();
         
      assertNull(message);
      
      assertTrue(interceptor1.wasCalled());
      assertFalse(interceptor2.wasCalled());
      assertTrue(interceptor3.wasCalled());
      assertFalse(interceptor4.wasCalled());
           
      session.close();
   }
   
   public void testClientMultipleInterceptors() throws Exception
   {
      MyInterceptor6 interceptor1 = new MyInterceptor6("a", 1);
      MyInterceptor6 interceptor2 = new MyInterceptor6("b", 2);
      MyInterceptor6 interceptor3 = new MyInterceptor6("c", 3);
      MyInterceptor6 interceptor4 = new MyInterceptor6("d", 4);
      
      ClientSessionFactory sf = createInVMFactory();
      
      sf.addInterceptor(interceptor1);
      sf.addInterceptor(interceptor2);
      sf.addInterceptor(interceptor3);
      sf.addInterceptor(interceptor4);
      
      ClientSession session = sf.createSession(false, true, true, true);

      session.createQueue(QUEUE, QUEUE, null, false);

      ClientProducer producer = session.createProducer(QUEUE);

      final int numMessages = 10;
            
      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message = session.createClientMessage(false);
         
         producer.send(message);
      }

      ClientConsumer consumer = session.createConsumer(QUEUE);
      
      session.start();
      
      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message = consumer.receive(1000);
         
         assertEquals(1, message.getIntProperty("a").intValue());
         assertEquals(2, message.getIntProperty("b").intValue());
         assertEquals(3, message.getIntProperty("c").intValue());
         assertEquals(4, message.getIntProperty("d").intValue());
      }
      
      sf.removeInterceptor(interceptor2);
      
      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message = session.createClientMessage(false);
         
         producer.send(message);
      }

      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message = consumer.receive(1000);
         
         assertEquals(1, message.getIntProperty("a").intValue());
         assertFalse(message.containsProperty("b"));
         assertEquals(3, message.getIntProperty("c").intValue());
         assertEquals(4, message.getIntProperty("d").intValue());
        
      }
      
      interceptor3.setReject(true);
      
      interceptor1.setWasCalled(false);
      interceptor2.setWasCalled(false);
      interceptor3.setWasCalled(false);
      interceptor4.setWasCalled(false);
      
      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message = session.createClientMessage(false);
         
         producer.send(message);
      }

      ClientMessage message = consumer.receive(100);
         
      assertNull(message);
      
      assertTrue(interceptor1.wasCalled());
      assertFalse(interceptor2.wasCalled());
      assertTrue(interceptor3.wasCalled());
      assertFalse(interceptor4.wasCalled());
           
      session.close();
   }


}
