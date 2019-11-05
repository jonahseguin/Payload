/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.base.sync;

import com.jonahseguin.payload.base.PayloadCallback;
import com.jonahseguin.payload.base.Service;
import com.jonahseguin.payload.base.network.NetworkPayload;
import com.jonahseguin.payload.base.type.Payload;
import com.jonahseguin.payload.base.type.PayloadData;

public interface SyncService<K, X extends Payload<K>, N extends NetworkPayload<K>, D extends PayloadData> extends Service {

    void prepareUpdate(X payload, PayloadCallback<X> callback);

    void update(K key);

    void uncache(K key);

}
