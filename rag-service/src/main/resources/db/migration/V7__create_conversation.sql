create table conversation
(
    id                uuid primary key,
    knowledge_base_id uuid        not null,
    title             varchar(255),
    created_at        timestamptz not null,
    updated_at        timestamptz not null
);

create index idx_conversation_knowledge_base_id
    on conversation (knowledge_base_id);

create index idx_conversation_updated_at
    on conversation (updated_at desc);

create table conversation_message
(
    id              uuid primary key,
    conversation_id uuid        not null,
    role            varchar(32) not null
        constraint chk_conversation_message_role
            check (role in ('USER', 'ASSISTANT')),
    content         text        not null,
    created_at      timestamptz not null
);

create index idx_conversation_message_conversation_created
    on conversation_message (conversation_id, created_at);
