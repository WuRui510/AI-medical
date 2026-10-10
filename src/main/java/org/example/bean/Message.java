package org.example.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import nonapi.io.github.classgraph.json.Id;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document("chat_message")
public class Message {

    // mongodb的id
    @Id
    private ObjectId id;

    // 消息id
    private long messageId;

    // 消息内容（json列表）
    private String content;

}
