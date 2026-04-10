CREATE TABLE public.pages (
    id UUID NOT NULL PRIMARY KEY,
    created_at TIMESTAMP WITHOUT TIME ZONE,
    created_by UUID,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    updated_by UUID,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    title VARCHAR(255) NOT NULL,
    content TEXT,
    icon VARCHAR(255),
    parent_id UUID,
    project_id UUID NOT NULL,
    tenant_id UUID NOT NULL
);

CREATE INDEX idx_pages_project_id ON public.pages(project_id);
CREATE INDEX idx_pages_tenant_id ON public.pages(tenant_id);
CREATE INDEX idx_pages_parent_id ON public.pages(parent_id);
