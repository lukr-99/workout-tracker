using WorkoutTracker.App.ViewModels;

namespace WorkoutTracker.App.Pages;

public partial class ExerciseCatalogPage : ContentPage
{
    private readonly ExerciseCatalogViewModel _viewModel;

    public ExerciseCatalogPage()
    {
        InitializeComponent();
        _viewModel = ServiceHelper.GetService<ExerciseCatalogViewModel>();
        BindingContext = _viewModel;
    }

    protected override async void OnAppearing()
    {
        base.OnAppearing();
        await _viewModel.RefreshAsync();
    }
}
