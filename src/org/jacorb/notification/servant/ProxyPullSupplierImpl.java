package org.jacorb.notification.servant;

/*
 *        JacORB - a free Java ORB
 *
 *   Copyright (C) 1997-2003  Gerald Brose.
 *
 *   This library is free software; you can redistribute it and/or
 *   modify it under the terms of the GNU Library General Public
 *   License as published by the Free Software Foundation; either
 *   version 2 of the License, or (at your option) any later version.
 *
 *   This library is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *   Library General Public License for more details.
 *
 *   You should have received a copy of the GNU Library General Public
 *   License along with this library; if not, write to the Free
 *   Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */


import java.util.List;

import org.jacorb.notification.interfaces.Message;
import org.jacorb.notification.interfaces.MessageConsumer;

import org.omg.CORBA.Any;
import org.omg.CORBA.BooleanHolder;
import org.omg.CORBA.ORB;
import org.omg.CORBA.UNKNOWN;
import org.omg.CosEventChannelAdmin.AlreadyConnected;
import org.omg.CosEventComm.Disconnected;
import org.omg.CosEventComm.PullConsumer;
import org.omg.CosNotification.UnsupportedQoS;
import org.omg.CosNotifyChannelAdmin.ConsumerAdmin;
import org.omg.CosNotifyChannelAdmin.ProxyPullSupplierOperations;
import org.omg.CosNotifyChannelAdmin.ProxyPullSupplierPOATie;
import org.omg.CosNotifyChannelAdmin.ProxySupplierHelper;
import org.omg.CosNotifyChannelAdmin.ProxyType;
import org.omg.PortableServer.Servant;
import org.jacorb.notification.ChannelContext;
import org.jacorb.notification.PropertyManager;
import org.jacorb.notification.CollectionsWrapper;

/**
 * @author Alphonse Bendt
 * @version $Id$
 */

public class ProxyPullSupplierImpl
    extends AbstractProxySupplier
    implements ProxyPullSupplierOperations {

    private static final Any sUndefinedAny;

    static {
        ORB _orb = ORB.init();

        sUndefinedAny = _orb.create_any();
    }

    ////////////////////////////////////////

    private PullConsumer pullConsumer_ = null;

    ////////////////////////////////////////

    ProxyPullSupplierImpl(ConsumerAdminTieImpl adminServant,
                          ChannelContext channelContext,
                          PropertyManager adminProperties,
                          PropertyManager qosProperties) throws UnsupportedQoS {

        super(adminServant,
              channelContext,
              adminProperties,
              qosProperties);

        init(adminProperties, qosProperties);
    }

    ProxyPullSupplierImpl(AbstractAdmin adminServant,
                          ChannelContext channelContext,
                          PropertyManager adminProperties,
                          PropertyManager qosProperties,
                          Integer key) throws UnsupportedQoS {

        super(adminServant,
              channelContext,
              adminProperties,
              qosProperties,
              key);

        init(adminProperties, qosProperties);
    }

    ////////////////////////////////////////

    private void init(PropertyManager adminProperties,
                      PropertyManager qosProperties) throws UnsupportedQoS {

        setProxyType(ProxyType.PULL_ANY);
    }


    public void disconnect_pull_supplier() {
        dispose();
    }


    private void disconnect() {
        if (pullConsumer_ != null) {
            pullConsumer_.disconnect_pull_consumer();
            pullConsumer_ = null;
        }
    }


    public Any pull() throws Disconnected {
        checkConnected();

        try {
            Message _event = getMessageBlocking();
            try {
                return _event.toAny();
            } finally {
                _event.dispose();
            }
        } catch (InterruptedException e) {
            logger_.fatalError("interrupted", e);

            throw new UNKNOWN();
        }
    }


    public Any try_pull (BooleanHolder hasEvent)
        throws Disconnected {

        checkConnected();

        Any event = sUndefinedAny;
        hasEvent.value = false;

        Message _message = getMessageNoBlock();

        if (_message != null) {
            try {
                hasEvent.value = true;
                return _message.toAny();
            } finally {
                _message.dispose();
            }
        } else {
            hasEvent.value = false;
            return sUndefinedAny;
        }
    }


    /**
     * Deliver Event to the underlying Consumer. As our Consumer is a
     * PullConsumer we simply put the Events in a Queue. The
     * PullConsumer will pull the Events out of the Queue at a later time.
     */
    public void deliverMessage(Message message) {
        enqueue(message);
    }


    public void connect_any_pull_consumer(PullConsumer consumer)
        throws AlreadyConnected {

        logger_.info("connect any_pull_consumer");

        if (connected_) {
            throw new AlreadyConnected();
        }

        connected_ = true;
        pullConsumer_ = consumer;
    }


    public ConsumerAdmin MyAdmin() {
        return (ConsumerAdmin)myAdmin_.getCorbaRef();
    }


    public List getSubsequentFilterStages() {
        return CollectionsWrapper.singletonList(this);
    }


    public MessageConsumer getMessageConsumer() {
        return this;
    }


    public boolean hasMessageConsumer() {
        return true;
    }


    public void dispose() {
        super.dispose();
        disconnect();
        // pendingEvents_.clear();
    }

    public void enableDelivery() {
        // as delivery to this PullSupplier causes no remote calls
        // we can ignore this
    }


    public void disableDelivery() {
        // as delivery to this PullSupplier causes no remote calls
        // we can ignore this
    }


    public void deliverPendingMessages() {
        // as we do not actively deliver events we can ignore this
    }


    public synchronized Servant getServant() {
        if (thisServant_ == null) {
            thisServant_ = new ProxyPullSupplierPOATie(this);
        }
        return thisServant_;
    }


    public org.omg.CORBA.Object getCorbaRef() {
        return ProxySupplierHelper.narrow(getServant()._this_object(getORB()));
    }
}