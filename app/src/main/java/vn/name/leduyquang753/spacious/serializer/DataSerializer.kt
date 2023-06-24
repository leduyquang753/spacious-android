package vn.name.leduyquang753.spacious.serializer;

import androidx.datastore.core.CorruptionException;
import androidx.datastore.core.Serializer;
import com.google.protobuf.InvalidProtocolBufferException;
import java.io.InputStream;
import java.io.OutputStream;
import vn.name.leduyquang753.spacious.Date;
import vn.name.leduyquang753.spacious.proto.Data;
import vn.name.leduyquang753.spacious.proto.data;

object DataSerializer: Serializer<Data> {
	override val defaultValue: Data = data {
		enableNotifications = false;
		notificationTime = 800;
		lastNotificationDate = Date.today().index;
		nextId = 1L;
	}

	override suspend fun readFrom(input: InputStream): Data {
		try {
			return Data.parseFrom(input);
		} catch (e: InvalidProtocolBufferException) {
			throw CorruptionException("The app's data is corrupted.", e);
		}
	}

	override suspend fun writeTo(t: Data, output: OutputStream) {
		t.writeTo(output);
	}
}