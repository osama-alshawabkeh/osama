package main

import (
	"context"
	"log"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/gofiber/fiber/v2"
	"github.com/jackc/pgx/v5/pgxpool"
)

func main() {
	cfg := loadConfig()
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()

	pool, err := pgxpool.New(ctx, cfg.DatabaseURL)
	if err != nil {
		log.Fatalf("database configuration: %v", err)
	}
	defer pool.Close()

	if err := pool.Ping(ctx); err != nil {
		log.Fatalf("database is not reachable: %v", err)
	}

	app := fiber.New(fiber.Config{
		AppName:      "MultiChoice Backend",
		ReadTimeout:  15 * time.Second,
		WriteTimeout: 15 * time.Second,
		IdleTimeout:  60 * time.Second,
		BodyLimit:    2 * 1024 * 1024,
	})

	registerRoutes(app, pool, cfg)

	go func() {
		if err := app.Listen(cfg.ListenAddress); err != nil {
			log.Printf("HTTP server stopped: %v", err)
		}
	}()

	stop := make(chan os.Signal, 1)
	signal.Notify(stop, os.Interrupt, syscall.SIGTERM)
	<-stop

	shutdownCtx, shutdownCancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer shutdownCancel()
	if err := app.ShutdownWithContext(shutdownCtx); err != nil {
		log.Printf("graceful shutdown: %v", err)
	}
}

type Config struct {
	ListenAddress string
	DatabaseURL   string
}

func loadConfig() Config {
	addr := os.Getenv("MC_LISTEN_ADDR")
	if addr == "" {
		addr = "127.0.0.1:8080"
	}
	dbURL := os.Getenv("MC_DATABASE_URL")
	if dbURL == "" {
		dbURL = "postgres://multichoice:CHANGE_THIS_PASSWORD@127.0.0.1:5432/multichoice?sslmode=disable"
	}
	return Config{ListenAddress: addr, DatabaseURL: dbURL}
}

func registerRoutes(app *fiber.App, pool *pgxpool.Pool, cfg Config) {
	app.Get("/health", func(c *fiber.Ctx) error {
		return c.JSON(fiber.Map{
			"status": "ok",
			"service": "multichoice-backend",
			"version": "0.1.0",
		})
	})

	app.Get("/ready", func(c *fiber.Ctx) error {
		ctx, cancel := context.WithTimeout(c.UserContext(), 2*time.Second)
		defer cancel()
		if err := pool.Ping(ctx); err != nil {
			return c.Status(fiber.StatusServiceUnavailable).JSON(fiber.Map{
				"status": "not_ready",
				"database": "unavailable",
			})
		}
		return c.JSON(fiber.Map{"status": "ready", "database": "connected"})
	})

	app.Get("/api/v1", func(c *fiber.Ctx) error {
		return c.JSON(fiber.Map{
			"name": "MultiChoice Backend API",
			"version": "v1",
			"docs": "API documentation will be added with the authentication and content modules.",
		})
	})
}
