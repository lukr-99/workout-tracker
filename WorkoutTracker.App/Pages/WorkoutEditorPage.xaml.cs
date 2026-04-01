using WorkoutTracker.App.ViewModels;

namespace WorkoutTracker.App.Pages;

public partial class WorkoutEditorPage : ContentPage, IQueryAttributable
{
    private readonly WorkoutEditorViewModel _viewModel;

    public WorkoutEditorPage()
    {
        InitializeComponent();
        _viewModel = ServiceHelper.GetService<WorkoutEditorViewModel>();
        BindingContext = _viewModel;
    }

    public async void ApplyQueryAttributes(IDictionary<string, object> query)
    {
        if (query.TryGetValue("sessionId", out var raw) && raw is string sessionId)
        {
            await _viewModel.LoadAsync(sessionId);
        }
    }

    protected override void OnDisappearing()
    {
        base.OnDisappearing();
        _viewModel.StopTimer();
    }
}
