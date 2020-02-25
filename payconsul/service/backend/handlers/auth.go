package handlers

import (
	"database/sql"
	_ "github.com/mattn/go-sqlite3"
	"github.com/sirupsen/logrus"
	"github.com/spf13/viper"
	"net/http"
)

type User struct {
	Name string
	Quota int64
}

func CheckAuth (r *http.Request) *User {
	sessID, err := r.Cookie("session_id")
	if err != nil {
		return nil
	}

	if len(sessID.Value) == 0 {
		return nil
	}

	db, err := sql.Open("sqlite3", viper.GetString("DSN"))
	if err != nil {
		logrus.WithError(err).Errorf("Can't open database")
		return nil
	}

	defer db.Close()
	row := db.QueryRow("SELECT name, quota FROM users WHERE session = $1", sessID.Value)
	user := &User{}

	err = row.Scan(&user.Name, &user.Quota)
	if err != nil {
		logrus.WithError(err).Errorf("Can't scan user")
		return nil
	}

	return user
}
