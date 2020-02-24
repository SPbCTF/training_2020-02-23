create database pagila;

\c pagila;

CREATE TABLE "users" (
  "id"  SERIAL PRIMARY KEY,
  "fname" varchar(100) NOT NULL DEFAULT '',
  "lname" varchar(100) DEFAULT NULL,
  "about_private" boolean DEFAULT NULL,
  "bdate" date DEFAULT NULL,
  "username" varchar(50) NOT NULL DEFAULT '',
  "ap_url" varchar(300) DEFAULT NULL,
  "ap_inbox" varchar(300) DEFAULT NULL,
  "ap_outbox" varchar(300) DEFAULT NULL,
  "ap_shared_inbox" varchar(300) DEFAULT NULL,
  "about" text,
  "gender" smallint NOT NULL DEFAULT '0',
  "profile_fields" text,
  "avatar" text,
  "ap_id" varchar(300) DEFAULT NULL UNIQUE,
  "ap_followers" varchar(300) DEFAULT NULL,
  "ap_following" varchar(300) DEFAULT NULL,
  "last_updated" timestamp NULL DEFAULT NULL,
  "created_at" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "flags" bigint NOT NULL DEFAULT 0,
  UNIQUE ("username")
) ;

CREATE TABLE "accounts" (
  "id" SERIAL PRIMARY KEY ,
  "user_id" bigint  NOT NULL,
  "email" varchar(200) NOT NULL DEFAULT '',
  "password" bytea DEFAULT NULL,
  "created_at" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "invited_by" bigint DEFAULT NULL,
  "access_level" smallint NOT NULL DEFAULT '1',
  "preferences" text,
  FOREIGN KEY (user_id) REFERENCES users (id) on delete cascade

) ;

CREATE TABLE "followings" (
  "follower_id" bigint ,
  "followee_id" bigint ,
  "mutual" boolean NOT NULL DEFAULT '0',
  "accepted" boolean NOT NULL DEFAULT '1',
  FOREIGN KEY (followee_id) REFERENCES users (id) on delete cascade,
  FOREIGN KEY (follower_id) REFERENCES users (id) on delete cascade
) ;


CREATE TABLE "friend_requests" (
  "from_user_id" bigint NOT NULL,
  "to_user_id" bigint NOT NULL,
  "message" text,
  FOREIGN KEY (from_user_id) REFERENCES users (id) on delete cascade,
  FOREIGN KEY (to_user_id) REFERENCES users (id) on delete cascade
) ;


CREATE TABLE "likes" (
  "user_id" bigint NOT NULL,
  "object_id" bigint NOT NULL,
  "object_type" bigint NOT NULL,
  FOREIGN KEY ("user_id") REFERENCES users (id),
  UNIQUE (user_id, object_id, object_type)
) ;

CREATE TABLE "media_cache" (
  "url_hash" bytea NOT NULL,
  "size" bigint NOT NULL,
  "last_access" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "info" bytea,
  "type" smallint NOT NULL,
  PRIMARY KEY ("url_hash")
) ;

CREATE TABLE "newsfeed" (
  "type" bigint NOT NULL,
  "author_id" bigint NOT NULL,
  "object_id" bigint DEFAULT NULL,
  "time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (type, author_id, object_id)
) ;


CREATE TABLE "servers" (
  "host" varchar(100) NOT NULL DEFAULT '',
  "software" varchar(100) DEFAULT NULL,
  "version" varchar(30) DEFAULT NULL,
  "capabilities" bigint  NOT NULL DEFAULT '0',
  "last_updated" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY ("host")
) ;

CREATE TABLE "signup_invitations" (
  "code" bytea NOT NULL ,
  "owner_id" bigint DEFAULT NULL,
  "created" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "signups_remaining" bigint NOT NULL,
  "hidden" boolean not null default false,
  PRIMARY KEY ("code"),
  FOREIGN KEY (owner_id) REFERENCES accounts ("id") ON DELETE  cascade
) ;


CREATE TABLE "wall_posts" (
  "id" SERIAL PRIMARY KEY ,
  "author_id" bigint NOT NULL,
  "owner_user_id" bigint DEFAULT NULL,
  "owner_group_id" bigint DEFAULT NULL,
  "text" text NOT NULL,
  "attachments" text ,
  "repost_of" bigint DEFAULT NULL,
  "ap_url" varchar(300) DEFAULT NULL,
  "ap_id" varchar(300) ,
  "created_at" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "content_warning" text,
  "updated_at" timestamp NULL DEFAULT NULL,
  "reply_to" bigint DEFAULT NULL,
  "reply_level" bigint DEFAULT '0',
  "reply_top_level_post" bigint DEFAULT NULL,
  "reply_key" bytea DEFAULT NULL,
  "mentions" bytea DEFAULT NULL,
  "private" boolean NOT NULL,
  UNIQUE (ap_id),
  FOREIGN KEY  (author_id) REFERENCES users ("id") ON DELETE  CASCADE ,
  FOREIGN KEY  (repost_of) REFERENCES wall_posts ("id") ON DELETE  set null  ON UPDATE  NO ACTION ,
  FOREIGN KEY  (owner_user_id) REFERENCES users ("id") ON DELETE cascade
) ;


CREATE TABLE "sessions" (
  "id" bytea NOT NULL PRIMARY KEY ,
  "account_id" bigint NOT NULL,
  "last_active" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "last_ip" bytea DEFAULT NULL,
  FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE  CASCADE
);


-- this is basically for checker bootstrap
INSERT INTO users (id, fname, lname, about_private, bdate, username, ap_url, ap_inbox, ap_outbox, ap_shared_inbox, about, gender, profile_fields, avatar, ap_id, ap_followers, ap_following, last_updated, flags) VALUES (DEFAULT, 'Дружелюбный', 'Толян', true, null, 'korniltsev', null, null, null, null, '\_______(-_-)__/', 0, null, 'friendly_tolyan', null, null, null, null, 0);
INSERT INTO public.accounts (id, user_id, email, password, created_at, invited_by, access_level, preferences) VALUES (DEFAULT, (select id from users where username ='korniltsev'), 'korniltsev@gov.ua', E'\\x65E84BE33532FB784C48129675F9EFF3A682B27168C0EA744B2CF58EE02337C5', '2020-02-21 14:42:50.479208', null, 1, '{"tz":"GMT-03:00","lang":"ru-RU"}');
