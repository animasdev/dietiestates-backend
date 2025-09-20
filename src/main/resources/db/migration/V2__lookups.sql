-- Flyway V2: lookup tables and seed data
-- Enable pgcrypto for gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Roles
CREATE TABLE IF NOT EXISTS roles (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  code text UNIQUE NOT NULL,
  name text NOT NULL,
  description text NULL,
  created_at timestamptz NOT NULL DEFAULT now()
);

INSERT INTO roles (code, name, description) VALUES
  ('USER',  'Utente',  'Cliente della piattaforma'),
  ('AGENT', 'Agente',  'Agente immobiliare'),
  ('AGENCY', 'Agenzia',  'Agenzia immobiliare'),
  ('SUPERADMIN', 'SuperAdmin',  'Amministratore di sistema con diritti di creazione admin'),
  ('ADMIN', 'Admin',   'Amministratore di sistema')
ON CONFLICT (code) DO NOTHING;

-- Listing statuses
CREATE TABLE IF NOT EXISTS listing_statuses (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  code text UNIQUE NOT NULL,
  name text NOT NULL,
  sort_order int NULL,
  created_at timestamptz NOT NULL DEFAULT now()
);

INSERT INTO listing_statuses (code, name, sort_order) VALUES
  ('DRAFT',          'Bozza',             10),
  ('PUBLISHED',      'Pubblicato',        20),
  ('PENDING_DELETE', 'In attesa di eliminazione', 30),
  ('DELETED',        'Eliminato',         40)
ON CONFLICT (code) DO NOTHING;

-- Listing types
CREATE TABLE IF NOT EXISTS listing_types (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  code text UNIQUE NOT NULL,
  name text NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now()
);

INSERT INTO listing_types (code, name) VALUES
  ('RENT', 'Affitto'),
  ('SALE', 'Vendita')
ON CONFLICT (code) DO NOTHING;

-- Features (servizi)
CREATE TABLE IF NOT EXISTS features (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  code text UNIQUE NOT NULL,
  name text NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now()
);

INSERT INTO features (code, name) VALUES
  ('ELEVATOR',        'Ascensore'),
  ('DOORMAN',         'Portineria'),
  ('AIR_CONDITIONING','Climatizzazione')
ON CONFLICT (code) DO NOTHING;

-- Moderation action types
CREATE TABLE IF NOT EXISTS moderation_action_types (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  code text UNIQUE NOT NULL,
  name text NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now()
);

INSERT INTO moderation_action_types (code, name) VALUES
  ('EDIT',    'Modifica'),
  ('DELETE',  'Eliminazione'),
  ('RESTORE', 'Ripristino')
ON CONFLICT (code) DO NOTHING;

-- Entity types (per target generici di moderazione/audit)
CREATE TABLE IF NOT EXISTS entity_types (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  code text UNIQUE NOT NULL,
  name text NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now()
);

INSERT INTO entity_types (code, name) VALUES
  ('LISTING', 'Annuncio')
ON CONFLICT (code) DO NOTHING;
