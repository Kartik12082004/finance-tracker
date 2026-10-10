import { useMemo, useState } from "react";
import type { Category, CategoryType } from "../types/category";
import {
  createCategory,
  deleteCategory,
  updateCategory,
} from "../api/categoryApi";

type CategoriesProps = {
  categories: Category[];
  onCategoriesChanged: () => Promise<void>;
};

type CategoryFormState = {
  name: string;
  type: CategoryType;
  parentId: string;
};

function getChildren(categories: Category[], parentId: string | null) {
  return categories.filter((category) => category.parentId === parentId);
}

function CategoryRow({
  category,
  categories,
  onEdit,
  onDelete,
}: {
  category: Category;
  categories: Category[];
  onEdit: (category: Category) => void;
  onDelete: (category: Category) => void;
}) {
  const children = getChildren(categories, category.id);

  return (
    <div className="category-tree-node">
      <div className="category-row">
        <div className="category-row-main">
          <div className="category-name">{category.name}</div>

          {category.isDefault && (
            <span className="category-badge">Default</span>
          )}
        </div>

        {!category.isDefault && (
          <div className="category-actions">
            <button
              type="button"
              className="secondary-button"
              onClick={() => onEdit(category)}
            >
              Edit
            </button>

            <button
              type="button"
              className="danger-button"
              onClick={() => onDelete(category)}
            >
              Delete
            </button>
          </div>
        )}
      </div>

      {children.length > 0 && (
        <div className="category-children">
          {children.map((child) => (
            <CategoryRow
              key={child.id}
              category={child}
              categories={categories}
              onEdit={onEdit}
              onDelete={onDelete}
            />
          ))}
        </div>
      )}
    </div>
  );
}

export default function Categories({
  categories,
  onCategoriesChanged,
}: CategoriesProps) {
  const [showCreateForm, setShowCreateForm] = useState(false);

  const [editingCategory, setEditingCategory] = useState<Category | null>(null);

  const [form, setForm] = useState<CategoryFormState>({
    name: "",
    type: "EXPENSE",
    parentId: "",
  });

  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  const expenseCategories = useMemo(
    () => categories.filter((category) => category.type === "EXPENSE"),
    [categories],
  );

  const incomeCategories = useMemo(
    () => categories.filter((category) => category.type === "INCOME"),
    [categories],
  );

  const parentOptions = useMemo(
    () =>
      categories.filter(
        (category) =>
          category.type === form.type && category.id !== editingCategory?.id,
      ),
    [categories, form.type, editingCategory],
  );

  function resetForm() {
    setForm({
      name: "",
      type: "EXPENSE",
      parentId: "",
    });

    setEditingCategory(null);
    setShowCreateForm(false);
    setError(null);
  }

  function handleStartCreate() {
    setEditingCategory(null);

    setForm({
      name: "",
      type: "EXPENSE",
      parentId: "",
    });

    setError(null);
    setShowCreateForm(true);
  }

  function handleStartEdit(category: Category) {
    setEditingCategory(category);

    setForm({
      name: category.name,
      type: category.type,
      parentId: category.parentId ?? "",
    });

    setError(null);
    setShowCreateForm(false);
  }

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();

    setError(null);
    setSaving(true);

    try {
      const name = form.name.trim();

      if (!name) {
        setError("Category name is required.");
        return;
      }

      const parentId = form.parentId || null;

      if (editingCategory) {
        await updateCategory(editingCategory.id, {
          name,
          parentId,
        });
      } else {
        await createCategory({
          name,
          type: form.type,
          parentId,
        });
      }

      await onCategoriesChanged();
      resetForm();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unable to save category.");
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete(category: Category) {
    const confirmed = window.confirm(
      `Delete "${category.name}"? This cannot be undone.`,
    );

    if (!confirmed) {
      return;
    }

    setError(null);

    try {
      await deleteCategory(category.id);
      await onCategoriesChanged();
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "Unable to delete category.",
      );
    }
  }

  function renderCategorySection(
    title: string,
    type: CategoryType,
    sectionCategories: Category[],
  ) {
    const rootCategories = getChildren(sectionCategories, null);

    return (
      <section className="category-section">
        <div className="category-section-header">
          <div>
            <p className="eyebrow">
              {type === "EXPENSE" ? "Money out" : "Money in"}
            </p>

            <h3>{title}</h3>
          </div>

          <span className="category-count">
            {sectionCategories.length} categories
          </span>
        </div>

        {rootCategories.length === 0 ? (
          <p className="empty-state">No {type.toLowerCase()} categories yet.</p>
        ) : (
          <div className="category-tree">
            {rootCategories.map((category) => (
              <CategoryRow
                key={category.id}
                category={category}
                categories={sectionCategories}
                onEdit={handleStartEdit}
                onDelete={handleDelete}
              />
            ))}
          </div>
        )}
      </section>
    );
  }

  return (
    <div className="categories-page">
      <section className="page-section">
        <div className="section-heading">
          <div>
            <p className="eyebrow">Organize your money</p>

            <h2>Categories</h2>

            <p className="section-description">
              Manage the categories used to organize your income and expenses.
            </p>
          </div>

          <button
            type="button"
            className="primary-button"
            onClick={handleStartCreate}
          >
            + Add Category
          </button>
        </div>

        {error && (
          <div className="form-error" role="alert">
            {error}
          </div>
        )}

        {(showCreateForm || editingCategory) && (
          <form className="category-form" onSubmit={handleSubmit}>
            <div className="category-form-header">
              <div>
                <p className="eyebrow">
                  {editingCategory ? "Edit category" : "New category"}
                </p>

                <h3>
                  {editingCategory ? editingCategory.name : "Create a category"}
                </h3>
              </div>

              <button
                type="button"
                className="secondary-button"
                onClick={resetForm}
              >
                Cancel
              </button>
            </div>

            <div className="form-grid">
              <label>
                <span>Name</span>

                <input
                  type="text"
                  value={form.name}
                  maxLength={100}
                  placeholder="e.g. Travel"
                  onChange={(event) =>
                    setForm((current) => ({
                      ...current,
                      name: event.target.value,
                    }))
                  }
                  required
                />
              </label>

              {!editingCategory && (
                <label>
                  <span>Type</span>

                  <select
                    value={form.type}
                    onChange={(event) =>
                      setForm((current) => ({
                        ...current,
                        type: event.target.value as CategoryType,
                        parentId: "",
                      }))
                    }
                  >
                    <option value="EXPENSE">Expense</option>
                    <option value="INCOME">Income</option>
                  </select>
                </label>
              )}

              <label>
                <span>Parent category</span>

                <select
                  value={form.parentId}
                  onChange={(event) =>
                    setForm((current) => ({
                      ...current,
                      parentId: event.target.value,
                    }))
                  }
                >
                  <option value="">No parent</option>

                  {parentOptions.map((category) => (
                    <option key={category.id} value={category.id}>
                      {category.name}
                    </option>
                  ))}
                </select>
              </label>
            </div>

            <div className="category-form-actions">
              <button
                type="submit"
                className="primary-button"
                disabled={saving}
              >
                {saving
                  ? "Saving..."
                  : editingCategory
                    ? "Save Changes"
                    : "Create Category"}
              </button>
            </div>
          </form>
        )}
      </section>

      <section className="page-section">
        <div className="categories-grid">
          {renderCategorySection(
            "Expense Categories",
            "EXPENSE",
            expenseCategories,
          )}

          {renderCategorySection(
            "Income Categories",
            "INCOME",
            incomeCategories,
          )}
        </div>
      </section>
    </div>
  );
}
