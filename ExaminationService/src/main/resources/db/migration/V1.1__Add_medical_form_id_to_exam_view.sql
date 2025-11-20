-- Migration: Add medicalFormId column to exam_view table
-- Description: Support for tracking medical form ID associated with each examination
-- Version: 1.1
-- Date: 2025-11-20

-- Add medical_form_id column to exam_view
ALTER TABLE exam_view ADD COLUMN IF NOT EXISTS medical_form_id VARCHAR(255) NULL;

-- Create index on medical_form_id for faster lookups if needed
CREATE INDEX IF NOT EXISTS idx_exam_view_medical_form_id ON exam_view(medical_form_id);

-- Add comment to column (Optional, for documentation)
-- COMMENT ON COLUMN exam_view.medical_form_id IS 'ID of the medical form associated with this examination';

